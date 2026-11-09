package com.example.deliverygo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : BaseActivity(), EasyPermissions.PermissionCallbacks {

    companion object {
        private const val RC_LOCATION = 123

        // Délais (ms) entre les commandes simulées
        private const val FIRST_ORDER_DELAY = 10_000L
        private const val ORDER_INTERVAL = 30_000L
        private const val AFTER_REFUSE_DELAY = 15_000L
        private const val AFTER_FINISH_DELAY = 20_000L
    }

    private lateinit var mapView: MapView
    private lateinit var distanceText: TextView
    private lateinit var durationText: TextView
    private lateinit var statusText: TextView
    private lateinit var orderText: TextView
    private lateinit var deliveryPanel: LinearLayout
    private lateinit var finishButton: Button

    private lateinit var fusedClient: FusedLocationProviderClient
    private var driverMarker: Marker? = null
    private var routeLine: Polyline? = null
    private val orderOverlays = mutableListOf<Overlay>()

    private var tracking = false
    private var delivering = false
    private var lastLocation: Location? = null
    private var totalDistanceMeters = 0f
    private var startTime = 0L
    private var centered = false

    private var pendingOrder: Order? = null
    private var activeOrder: Order? = null
    private var nextOrderIndex = 0
    private var orderDialog: AlertDialog? = null

    private val handler = Handler(Looper.getMainLooper())

    private val timer = object : Runnable {
        override fun run() {
            updateDuration()
            handler.postDelayed(this, 1000)
        }
    }

    private val orderRunnable = Runnable { dispatchNextOrder() }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { onNewLocation(it) }
        }
    }

    // Tuiles satellite (imagerie réelle) Esri — sans clé API
    private val satelliteTiles = object : OnlineTileSourceBase(
        "EsriWorldImagery", 0, 19, 256, ".jpg",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String =
            baseUrl +
                    MapTileIndex.getZoom(pMapTileIndex) + "/" +
                    MapTileIndex.getY(pMapTileIndex) + "/" +
                    MapTileIndex.getX(pMapTileIndex)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_main)
        setupNavigation(R.id.nav_home)

        mapView = findViewById(R.id.mapView)
        distanceText = findViewById(R.id.distanceText)
        durationText = findViewById(R.id.durationText)
        statusText = findViewById(R.id.statusText)
        orderText = findViewById(R.id.orderText)
        deliveryPanel = findViewById(R.id.deliveryPanel)
        finishButton = findViewById(R.id.finishButton)

        mapView.setTileSource(satelliteTiles)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(33.9716, -6.8498)) // Rabat par défaut

        // Marqueur 🛵 visible dès l'ouverture (avant le premier fix GPS)
        driverMarker = Marker(mapView).apply {
            position = GeoPoint(33.9716, -6.8498)
            icon = emojiIcon("🛵")
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = getString(R.string.driver_marker)
        }
        mapView.overlays.add(driverMarker)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        finishButton.setOnClickListener { finishDelivery() }

        NotificationHelper.createChannel(this)
        requestLocationPermission()

        scheduleNextOrder(FIRST_ORDER_DELAY)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    // ---------------- Permissions ----------------

    private fun requestLocationPermission() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            startLocationUpdates()
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.location_rationale),
                RC_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == RC_LOCATION) {
            startLocationUpdates()
            NotificationHelper.requestPermission(this)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (requestCode == RC_LOCATION) {
            NotificationHelper.requestPermission(this)
        }
    }

    // ---------------- GPS ----------------

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (tracking) return
        tracking = true

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) onNewLocation(location)
        }
    }

    private fun onNewLocation(location: Location) {
        val point = GeoPoint(location.latitude, location.longitude)

        if (driverMarker == null) {
            driverMarker = Marker(mapView).apply {
                icon = emojiIcon("🛵")
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = getString(R.string.driver_marker)
            }
            mapView.overlays.add(driverMarker)
        }
        driverMarker?.position = point

        if (!centered) {
            mapView.controller.setZoom(17.0)
            mapView.controller.animateTo(point)
            centered = true
        }

        if (delivering) {
            lastLocation?.let { totalDistanceMeters += it.distanceTo(location) }
            routeLine?.addPoint(point)
            updateDistance()
        }
        lastLocation = location
        mapView.invalidate()
    }

    /** Remet le marqueur du livreur au-dessus des autres éléments de la carte. */
    private fun bringDriverToFront() {
        driverMarker?.let {
            mapView.overlays.remove(it)
            mapView.overlays.add(it)
        }
    }

    // ---------------- Commandes ----------------

    private fun scheduleNextOrder(delayMs: Long) {
        handler.removeCallbacks(orderRunnable)
        handler.postDelayed(orderRunnable, delayMs)
    }

    /** Une nouvelle commande arrive : notification (ou boîte de dialogue si notifications bloquées). */
    private fun dispatchNextOrder() {
        if (activeOrder != null || isFinishing) return

        val order = OrderRepository.orders[nextOrderIndex % OrderRepository.orders.size]
        nextOrderIndex++
        pendingOrder = order

        if (NotificationHelper.canNotify(this)) {
            NotificationHelper.showOrder(this, order)
        } else {
            showOrderDialog(order)
        }
        scheduleNextOrder(ORDER_INTERVAL)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getIntExtra(NotificationHelper.EXTRA_ORDER_ID, -1) ?: -1
        val order = OrderRepository.byId(id)

        if (order != null) {
            when (intent?.action) {
                NotificationHelper.ACTION_ACCEPT_ORDER -> acceptOrder(order)
                NotificationHelper.ACTION_REFUSE_ORDER -> refuseOrder()
                NotificationHelper.ACTION_OPEN_ORDER ->
                    if (activeOrder == null) showOrderDialog(order)
            }
        }
        // évite de rejouer l'action si l'activité est recréée (ex. changement de langue)
        setIntent(Intent())
    }

    private fun showOrderDialog(order: Order) {
        if (isFinishing) return
        orderDialog?.dismiss()
        orderDialog = AlertDialog.Builder(this)
            .setTitle(R.string.order_notification_title)
            .setMessage(
                getString(R.string.order_notification_text, order.clientName, order.pickupName)
            )
            .setCancelable(true)
            .setPositiveButton(R.string.order_accept) { _, _ -> acceptOrder(order) }
            .setNegativeButton(R.string.order_refuse) { _, _ -> refuseOrder() }
            .show()
    }

    private fun acceptOrder(order: Order) {
        if (activeOrder != null) return

        orderDialog?.dismiss()
        NotificationHelper.cancelOrder(this)
        handler.removeCallbacks(orderRunnable)

        pendingOrder = null
        activeOrder = order

        showOrderOnMap(order)
        orderText.text = getString(
            R.string.order_card_format, order.pickupName, order.clientName, order.clientAddress
        )

        startDelivery()
    }

    private fun refuseOrder() {
        orderDialog?.dismiss()
        NotificationHelper.cancelOrder(this)
        pendingOrder = null
        // d'autres commandes continueront d'arriver
        scheduleNextOrder(AFTER_REFUSE_DELAY)
    }

    private fun showOrderOnMap(order: Order) {
        clearOrderOnMap()

        val pickup = GeoPoint(order.pickupLat, order.pickupLng)
        val client = GeoPoint(order.clientLat, order.clientLng)

        val pickupMarker = Marker(mapView).apply {
            position = pickup
            icon = emojiIcon("🏪")
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = getString(R.string.order_pickup_marker, order.pickupName)
        }
        val clientMarker = Marker(mapView).apply {
            position = client
            icon = emojiIcon("🏠")
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = getString(R.string.order_client_marker, order.clientName)
        }
        val line = Polyline().apply {
            outlinePaint.color = Color.rgb(255, 152, 0)
            addPoint(pickup)
            addPoint(client)
        }

        orderOverlays.add(line)
        orderOverlays.add(pickupMarker)
        orderOverlays.add(clientMarker)
        mapView.overlays.addAll(orderOverlays)
        bringDriverToFront()
        pickupMarker.showInfoWindow()

        centered = true
        val points = mutableListOf(pickup, client)
        driverMarker?.position?.let { points.add(it) }
        mapView.post {
            mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 100)
        }
        mapView.invalidate()
    }

    private fun clearOrderOnMap() {
        InfoWindow.closeAllInfoWindowsOn(mapView)
        mapView.overlays.removeAll(orderOverlays.toSet())
        orderOverlays.clear()
        mapView.invalidate()
    }

    // ---------------- Livraison ----------------

    private fun startDelivery() {
        delivering = true
        totalDistanceMeters = 0f
        startTime = SystemClock.elapsedRealtime()

        routeLine?.let { mapView.overlays.remove(it) }
        routeLine = Polyline().apply { outlinePaint.color = Color.BLUE }
        mapView.overlays.add(routeLine)
        lastLocation?.let { routeLine?.addPoint(GeoPoint(it.latitude, it.longitude)) }
        bringDriverToFront()

        deliveryPanel.visibility = View.VISIBLE
        statusText.setText(R.string.status_delivering)

        updateDistance()
        updateDuration()
        handler.removeCallbacks(timer)
        handler.postDelayed(timer, 1000)
    }

    private fun finishDelivery() {
        delivering = false
        handler.removeCallbacks(timer)

        val km = totalDistanceMeters / 1000.0
        val seconds = (SystemClock.elapsedRealtime() - startTime) / 1000
        val minutes = seconds / 60
        val rest = seconds % 60

        AlertDialog.Builder(this)
            .setTitle(R.string.summary_title)
            .setMessage(getString(R.string.summary_message, km, minutes, rest))
            .setPositiveButton(android.R.string.ok, null)
            .show()

        NotificationHelper.show(
            this,
            getString(R.string.notification_title),
            getString(R.string.delivery_finished_notification, km)
        )

        clearOrderOnMap()
        activeOrder = null
        deliveryPanel.visibility = View.GONE

        // la prochaine commande arrivera bientôt
        scheduleNextOrder(AFTER_FINISH_DELAY)
    }

    private fun updateDistance() {
        distanceText.text = getString(R.string.distance_format, totalDistanceMeters / 1000.0)
    }

    private fun updateDuration() {
        if (!delivering) return
        val seconds = (SystemClock.elapsedRealtime() - startTime) / 1000
        durationText.text = getString(R.string.duration_format, seconds / 60, seconds % 60)
    }

    /** Transforme un emoji en icône de marqueur pour la carte. */
    private fun emojiIcon(emoji: String, sizeDp: Int = 36): BitmapDrawable {
        val px = (sizeDp * resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = px * 0.8f
            textAlign = Paint.Align.CENTER
        }
        val y = px / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(emoji, px / 2f, y, paint)
        return BitmapDrawable(resources, bitmap)
    }

    // ---------------- Cycle de vie ----------------

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(timer)
        handler.removeCallbacks(orderRunnable)
        orderDialog?.dismiss()
        if (tracking) fusedClient.removeLocationUpdates(locationCallback)
        mapView.onDetach()
        super.onDestroy()
    }
}