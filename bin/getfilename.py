import os

def write_filenames_to_text(directory_path, output_file):
    try:
        # Get list of all files in the directory
        filenames = [f for f in os.listdir(directory_path) if os.path.isfile(os.path.join(directory_path, f))]

        # Write filenames to the output text file
        with open(output_file, 'w', encoding='utf-8') as f:
            for filename in filenames:
                f.write(filename + '\n')

        print(f"Successfully wrote {len(filenames)} filenames to '{output_file}'")

    except Exception as e:
        print(f"Error: {e}")

# Example usage
write_filenames_to_text(r"D:\testing\14-test images\resources\ComponentBox", "image_list.txt")
