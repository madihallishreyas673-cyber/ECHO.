package com.focusverse.ai

data class Fm(
    val subject: String,
    val chapter: String,
    val name: String,
    val expr: String,
    val note: String
)

object Content {
    val subjects = listOf("Physics", "Chemistry", "Mathematics")

    val formulas: List<Fm> = listOf(
        // ---------- Physics ----------
        Fm("Physics", "Laws of Motion", "Newton's second law", "F = ma", "F = net force (N), m = mass (kg), a = acceleration (m/s²)."),
        Fm("Physics", "Laws of Motion", "Linear momentum", "p = mv", "Unit: kg·m/s. Momentum is a vector in the direction of velocity."),
        Fm("Physics", "Work, Energy and Power", "Kinetic energy", "KE = ½mv²", "m = mass (kg), v = speed (m/s). Unit: joule (J)."),
        Fm("Physics", "Work, Energy and Power", "Gravitational potential energy", "U = mgh", "h = height above the reference level, g ≈ 9.8 m/s². Near Earth's surface."),
        Fm("Physics", "Work, Energy and Power", "Work done by a constant force", "W = Fd cosθ", "θ = angle between force and displacement. Unit: joule (J)."),
        Fm("Physics", "Work, Energy and Power", "Power", "P = W/t", "Rate of doing work. Unit: watt (W)."),
        Fm("Physics", "Kinematics", "First equation of motion", "v = u + at", "u = initial velocity, v = final velocity, a = constant acceleration, t = time."),
        Fm("Physics", "Kinematics", "Second equation of motion", "s = ut + ½at²", "s = displacement under constant acceleration."),
        Fm("Physics", "Kinematics", "Third equation of motion", "v² = u² + 2as", "Use when time is not given."),
        Fm("Physics", "Circular Motion", "Centripetal force", "F = mv²/r", "r = radius of the circular path. Directed towards the centre."),
        Fm("Physics", "Electrostatics", "Coulomb's law", "F = kq₁q₂/r²", "k ≈ 9 × 10⁹ N·m²/C² in vacuum. Force between two point charges."),
        Fm("Physics", "Oscillations", "Time period of a simple pendulum", "T = 2π√(l/g)", "l = length (m). Valid for small angles of swing."),
        Fm("Physics", "Ray Optics", "Lens formula", "1/v − 1/u = 1/f", "Use the Cartesian sign convention for u, v and f."),
        Fm("Physics", "Current Electricity", "Ohm's law", "V = IR", "V in volts, I in amperes, R in ohms. Holds for ohmic conductors."),
        Fm("Physics", "Current Electricity", "Electric power", "P = VI", "Also P = I²R = V²/R."),
        Fm("Physics", "Properties of Matter", "Density", "ρ = m/V", "Unit: kg/m³."),
        // ---------- Chemistry ----------
        Fm("Chemistry", "States of Matter", "Ideal gas equation", "PV = nRT", "R = 8.314 J/(mol·K). T must be in kelvin."),
        Fm("Chemistry", "States of Matter", "Boyle's law", "P₁V₁ = P₂V₂", "At constant temperature and amount of gas."),
        Fm("Chemistry", "States of Matter", "Charles's law", "V₁/T₁ = V₂/T₂", "At constant pressure and amount of gas. T in kelvin."),
        Fm("Chemistry", "Some Basic Concepts", "Number of moles", "n = m/M", "m = given mass, M = molar mass."),
        Fm("Chemistry", "Some Basic Concepts", "Number of particles", "N = n × Nₐ", "Nₐ = 6.022 × 10²³ mol⁻¹ (Avogadro constant)."),
        Fm("Chemistry", "Solutions", "Molarity", "M = n(solute)/V(litres)", "Moles of solute per litre of solution. Unit: mol/L."),
        Fm("Chemistry", "Solutions", "Dilution formula", "M₁V₁ = M₂V₂", "Moles of solute stay the same on dilution."),
        Fm("Chemistry", "Equilibrium", "pH", "pH = −log[H⁺]", "[H⁺] in mol/L. pH < 7 is acidic at 25 °C."),
        Fm("Chemistry", "Thermodynamics", "Gibbs free energy", "ΔG = ΔH − TΔS", "A process is spontaneous when ΔG < 0 at constant T and P."),
        Fm("Chemistry", "Thermodynamics", "First law of thermodynamics", "ΔU = q + w", "IUPAC convention: w is work done ON the system."),
        Fm("Chemistry", "Electrochemistry", "Gibbs energy and cell EMF", "ΔG = −nFE", "F = 96485 C/mol, n = moles of electrons transferred."),
        Fm("Chemistry", "Chemical Kinetics", "First-order rate law", "rate = k[A]", "k has unit s⁻¹ for a first-order reaction."),
        // ---------- Mathematics ----------
        Fm("Mathematics", "Quadratic Equations", "Quadratic formula", "x = (−b ± √(b² − 4ac)) / 2a", "Roots of ax² + bx + c = 0, with a ≠ 0."),
        Fm("Mathematics", "Trigonometry", "Pythagorean identity", "sin²θ + cos²θ = 1", "True for every angle θ."),
        Fm("Mathematics", "Coordinate Geometry", "Distance formula", "d = √((x₂−x₁)² + (y₂−y₁)²)", "Distance between points (x₁, y₁) and (x₂, y₂)."),
        Fm("Mathematics", "Coordinate Geometry", "Slope of a line", "m = (y₂−y₁)/(x₂−x₁)", "Slope through two points; x₂ ≠ x₁."),
        Fm("Mathematics", "Sequences and Series", "Sum of n terms of an AP", "Sₙ = n/2 [2a + (n−1)d]", "a = first term, d = common difference."),
        Fm("Mathematics", "Sequences and Series", "Sum of n terms of a GP", "Sₙ = a(rⁿ − 1)/(r − 1)", "a = first term, r = common ratio, r ≠ 1."),
        Fm("Mathematics", "Differentiation", "Power rule of differentiation", "d/dx(xⁿ) = nxⁿ⁻¹", "Valid for any real n."),
        Fm("Mathematics", "Integration", "Power rule of integration", "∫xⁿ dx = xⁿ⁺¹/(n+1) + C", "Valid for n ≠ −1."),
        Fm("Mathematics", "Permutations and Combinations", "Combinations", "nCr = n!/(r!(n−r)!)", "Number of ways to choose r items from n, order ignored."),
        Fm("Mathematics", "Mensuration", "Volume of a sphere", "V = 4/3 πr³", "r = radius."),
        Fm("Mathematics", "Mensuration", "Area of a circle", "A = πr²", "r = radius."),
        Fm("Mathematics", "Logarithms", "Product rule of logarithms", "log(ab) = log a + log b", "a, b > 0.")
    )

    val quotes = listOf(
        "Small, steady study sessions beat last-minute cramming.",
        "Focus for 25 minutes. Then rest. Repeat.",
        "Every problem you solve today is one less surprise on exam day.",
        "Progress, not perfection.",
        "Your future self will thank you for today's effort.",
        "Understand it once, revise it often.",
        "Mistakes are the fastest teachers. Note them down."
    )

    val distractingApps = listOf(
        "com.instagram.android", "com.google.android.youtube", "com.facebook.katana",
        "com.snapchat.android", "com.twitter.android", "com.reddit.frontpage",
        "com.netflix.mediaclient", "in.mohalla.sharechat", "com.zhiliaoapp.musically",
        "com.pinterest", "com.discord", "tv.twitch.android.app"
    )
}
