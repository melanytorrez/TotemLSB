package com.ucb.app.totem.domain.repository

import com.ucb.app.totem.domain.model.Career

class CareerRepository {

    private val careers = mapOf(
        "A" to Career(
            letter = "A",
            name = "Administración de Empresas",
            faculty = "Ciencias Económicas y Financieras",
            description = "Forma líderes capaces de gestionar organizaciones con visión estratégica, innovación y responsabilidad social. Domina finanzas, marketing, operaciones y talento humano.",
            tags = listOf("Gestión", "Finanzas", "Liderazgo"),
            duration = "4 años",
            gradientColors = Pair(0xFF1A237E, 0xFF42A5F5)
        ),
        "B" to Career(
            letter = "B",
            name = "Bioquímica y Farmacia",
            faculty = "Ciencias de la Salud",
            description = "Estudia la composición química de los seres vivos y desarrolla fármacos para mejorar la salud. Combina laboratorio, investigación y atención farmacéutica.",
            tags = listOf("Laboratorio", "Farmacia", "Investigación"),
            duration = "5 años",
            gradientColors = Pair(0xFF00695C, 0xFF26A69A)
        ),
        "C" to Career(
            letter = "C",
            name = "Comunicación Social y Periodismo",
            faculty = "Humanidades",
            description = "Construye narrativas que transforman sociedades. Trabaja en medios digitales, periodismo de impacto y comunicación estratégica.",
            tags = listOf("Medios", "Periodismo", "Creatividad"),
            duration = "4 años",
            gradientColors = Pair(0xFF6A1B9A, 0xFFE040FB)
        ),
        "D" to Career(
            letter = "D",
            name = "Derecho",
            faculty = "Ciencias Jurídicas y Políticas",
            description = "Forma profesionales del derecho con pensamiento crítico y ético. Defiende la justicia y contribuye al desarrollo normativo de la sociedad.",
            tags = listOf("Justicia", "Leyes", "Ética"),
            duration = "5 años",
            gradientColors = Pair(0xFF4E342E, 0xFFBCAAA4)
        ),
        "E" to Career(
            letter = "E",
            name = "Economía",
            faculty = "Ciencias Económicas y Financieras",
            description = "Analiza mercados, políticas públicas y tendencias globales. Diseña estrategias económicas que impulsan el desarrollo sostenible de países y organizaciones.",
            tags = listOf("Análisis", "Mercados", "Políticas"),
            duration = "4 años",
            gradientColors = Pair(0xFF1B5E20, 0xFF66BB6A)
        ),
        "I" to Career(
            letter = "I",
            name = "Ingeniería de Sistemas",
            faculty = "Ingeniería",
            description = "Diseña, desarrolla e implementa soluciones tecnológicas. Desde inteligencia artificial hasta desarrollo de software y ciberseguridad.",
            tags = listOf("Software", "IA", "Tecnología"),
            duration = "5 años",
            gradientColors = Pair(0xFF0D47A1, 0xFF00E5FF)
        ),
        "M" to Career(
            letter = "M",
            name = "Medicina",
            faculty = "Ciencias de la Salud",
            description = "Forma médicos comprometidos con la salud integral. Diagnóstico, tratamiento y prevención de enfermedades con humanismo y excelencia científica.",
            tags = listOf("Salud", "Diagnóstico", "Ciencia"),
            duration = "6 años",
            gradientColors = Pair(0xFFB71C1C, 0xFFEF5350)
        ),
        "P" to Career(
            letter = "P",
            name = "Psicología",
            faculty = "Humanidades",
            description = "Comprende el comportamiento humano y promueve el bienestar emocional. Interviene en contextos clínicos, educativos y organizacionales.",
            tags = listOf("Mente", "Bienestar", "Terapia"),
            duration = "5 años",
            gradientColors = Pair(0xFF4A148C, 0xFFCE93D8)
        )
    )

    fun getCareerByLetter(letter: String): Career? {
        return careers[letter.uppercase()]
    }

    fun getAllLetters(): List<String> {
        return careers.keys.sorted()
    }

    fun getAllCareers(): List<Career> {
        return careers.values.sortedBy { it.letter }
    }
}
