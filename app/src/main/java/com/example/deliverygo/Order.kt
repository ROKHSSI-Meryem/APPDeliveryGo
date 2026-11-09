package com.example.deliverygo

data class Order(
    val id: Int,
    val clientName: String,
    val clientAddress: String,
    val clientLat: Double,
    val clientLng: Double,
    val pickupName: String,
    val pickupLat: Double,
    val pickupLng: Double
)

/** Commandes locales (sans base de données) : clients et lieux de récupération fixes. */
object OrderRepository {

    val orders = listOf(
        Order(1, "Yassine B.", "Hay Riad", 33.9606, -6.8686,
            "Restaurant Agdal", 33.9916, -6.8498),
        Order(2, "Salma E.", "Souissi", 33.9780, -6.8420,
            "Pharmacie Gare Rabat-Ville", 34.0158, -6.8330),
        Order(3, "Omar K.", "Océan", 34.0286, -6.8387,
            "Boulangerie Hassan", 34.0243, -6.8227),
        Order(4, "Imane R.", "Akkari", 34.0010, -6.8570,
            "Supermarché Agdal", 33.9930, -6.8510)
    )

    fun byId(id: Int): Order? = orders.firstOrNull { it.id == id }
}