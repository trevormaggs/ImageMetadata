# Image Metadata Parsing Toolkit – Development in Progress

Welcome to the **Image Metadata Parsing Toolkit** - a Java library dedicated to extracting and analysing **Exif metadata** from various image formats, specifically **TIFF, JPEG, PNG, HEIF/HEIC, and WebP**.

This project is a **development in progress**, aimed at showcasing my programming and documentation skills by building a robust and extensible image metadata parser. It demonstrates proficiency in Java, binary file parsing, metadata standards, and producing clear, comprehensive technical documentation. The library combines practical utility with clean, modular design to solve real-world problems in image metadata extraction.

Currently, the focus is exclusively on **Exif metadata** parsing, providing reliable access to detailed camera and image capture information embedded within images.

**No external libraries** are used at this stage - the entire implementation is built from the ground up using standard Java 8.

In future development phases, support will be expanded to include other metadata formats and standards commonly used in imaging workflows, such as **XMP, IPTC, and proprietary vendor metadata**.

## Key Features

* Accurate extraction of **Exif metadata** from **TIFF, JPEG, PNG, HEIF/HEIC, and WebP** files
* Low-level parsing of file structures to locate and decode Exif segments, chunks, or boxes
* Support for standard Exif tags with extensibility for custom metadata
* Core Java 8 implementation with **no external dependencies**
* Lightweight and modular design for efficient use in applications

## Getting Started

Clone the repository:

```bash
git clone https://github.com/trevormaggs/ImageMetadata.git
```

Import the project into your favourite Java IDE supporting Java 8 or above.

Review the **Javadoc documentation** for detailed API usage.

Use the library to parse Exif metadata from supported image files as needed.
