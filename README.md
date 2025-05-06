# Burp Network Schematics Editor Extension

## Overview
A Burp Suite extension for creating network diagrams and documentation directly within Burp. Features include:
- Drag-and-drop network element placement
- Connection line drawing
- Rich text editing with tables
- Export to PDF/PNG formats

## Requirements
- Burp Suite Professional/Community v2022.3+
- Java 8+ (same version as your Burp installation)

## Installation
1. Download the pre-built JAR from [Releases](#) (coming soon)
2. In Burp: Extender → Add → Select the downloaded JAR

## Compiling from Source

### Dependencies
1. **Burp API** (included in Burp):
   - Located in your Burp installation directory:
   ```
   burpsuite_pro.jar (or burpsuite_community.jar)
   ```

2. **Required Libraries**:
    Jython-https://www.jython.org/download.html
   - [iTextPDF 5.5.13]-(https://repo1.maven.org/maven2/com/itextpdf/itextpdf/5.5.13/itextpdf-5.5.13.jar)
   - [XMLWorker 5.5.13]-(https://repo1.maven.org/maven2/com/itextpdf/xmlworker/5.5.13/xmlworker-5.5.13.jar)

### Build Steps (Manual)
1. Download dependencies to `/lib` folder:
   ```bash
   mkdir lib
   wget -P lib https://repo1.maven.org/maven2/com/itextpdf/itextpdf/5.5.13/itextpdf-5.5.13.jar
   wget -P lib https://repo1.maven.org/maven2/com/itextpdf/xmlworker/5.5.13/xmlworker-5.5.13.jar
   ```

2. Compile:
   ```bash
   javac -cp "lib/*:burpsuite_pro.jar" src/burpeditor/*.java -d bin/
   ```

3. Create executable JAR:
   ```bash
   # Create manifest
   echo "Main-Class: burpeditor.BurpExtender" > MANIFEST.MF
   
   # Package with dependencies
   jar cvfm NetworkSchematicsEditor.jar MANIFEST.MF -C bin/ . -C lib/ .
   ```

## Usage
1. Open the "Schematics" tab in Burp
2. Use the toolbar to:
   - Add devices (drag from left panel)
   - Draw connections
   - Format documentation text
3. Export via:
   - File → Export → PDF/PNG

## Troubleshooting
**PDF Export Fails?**
1. Ensure all dependencies are properly packaged
2. Check Burp's Extender → Errors tab
3. Temporary workaround:
   - Place dependency JARs in `~/.BurpSuite/burp/extensions/`
   - Add them in Extender → Options → Java Environment

## License
Apache 2.0 - See [LICENSE](LICENSE)

---

**Note**: Replace `[Releases](#)` with your actual release URL when available. For Burp API, users should use the JAR from their own Burp installation to comply with PortSwigger's licensing terms.
