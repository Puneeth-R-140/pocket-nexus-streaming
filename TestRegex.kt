
import java.util.regex.Pattern

fun main() {
    val line = "#EXT-X-STREAM-INF:BANDWIDTH=3665400,RESOLUTION=1920x1080,FRAME-RATE=23.976,CODECS=\"avc1.640028,mp4a.40.2\""
    val bandwidth = extractAttribute(line, "BANDWIDTH")
    val resolution = extractAttribute(line, "RESOLUTION")
    
    println("Line: $line")
    println("Bandwidth: $bandwidth")
    println("Resolution: $resolution")
    
    val line2 = "#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=460560,RESOLUTION=480x270,NAME=\"480\""
    println("Line2: $line2")
    println("Bandwidth: ${extractAttribute(line2, "BANDWIDTH")}")
    println("Resolution: ${extractAttribute(line2, "RESOLUTION")}")
}

fun extractAttribute(line: String, attribute: String): String? {
    val pattern = "$attribute=\"([^\"]+)\""
    val matcher = Pattern.compile(pattern).matcher(line)
    if (matcher.find()) {
        return matcher.group(1)
    }
    
    // Try without quotes
    val pattern2 = "$attribute=([^,\\s]+)"
    val matcher2 = Pattern.compile(pattern2).matcher(line)
    if (matcher2.find()) {
        return matcher2.group(1)
    }
    return null
}
