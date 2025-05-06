import os
import shutil
from pathlib import Path

def copy_all_images_to_single_folder(root_dir, output_dir="images"):
    # Create output directory if it doesn't exist
    output_path = Path(output_dir)
    output_path.mkdir(exist_ok=True)
    
    # Supported image extensions
    image_extensions = {'.jpg', '.jpeg', '.png', '.gif', '.bmp', '.tiff', '.webp', '.svg'}
    
    # Counter for copied files
    copied_files = 0
    
    for foldername, subfolders, filenames in os.walk(root_dir):
        for filename in filenames:
            file_path = Path(foldername) / filename
            # Check if file is an image
            if file_path.suffix.lower() in image_extensions:
                # Create new filename to avoid duplicates
                new_filename = f"{file_path.stem}_{file_path.parent.name}{file_path.suffix}"
                output_file = output_path / new_filename
                
                # Copy the file
                shutil.copy2(file_path, output_file)
                copied_files += 1
                print(f"Copied: {file_path} -> {output_file}")
    
    print(f"\nDone! Copied {copied_files} images to {output_path.absolute()}")

if __name__ == "__main__":
    root_directory = input("Enter the root directory path: ").strip()
    copy_all_images_to_single_folder(root_directory)