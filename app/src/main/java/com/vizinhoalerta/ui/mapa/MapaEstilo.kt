package com.vizinhoalerta.ui.mapa

/** Estilo raster OpenStreetMap usado pelo MapLibre (sem API key). */
internal object MapaEstilo {
    val OSM = """
        {"version":8,"sources":{"osm":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"attribution":"© OpenStreetMap contributors"}},"layers":[{"id":"osm","type":"raster","source":"osm"}]}
    """.trimIndent()
}
