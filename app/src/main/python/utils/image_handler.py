import os
from PIL import Image
from io import BytesIO

def compress_image(image_path, max_size_kb=50):
    """Compresses image to binary for RNS transmission"""
    try:
        img = Image.open(image_path)
        
        # Convert to RGB (JPEG requirement)
        if img.mode != 'RGB':
            img = img.convert('RGB')
        
        # Downscale for LoRa/BT links
        img.thumbnail((480, 480), Image.Resampling.LANCZOS)
        
        # Compression loop
        quality = 70
        while quality > 10:
            buffer = BytesIO()
            img.save(buffer, format='JPEG', quality=quality, optimize=True)
            if buffer.tell() <= max_size_kb * 1024:
                break
            quality -= 5
            
        return buffer.getvalue() # Returns raw bytes
    except Exception as e:
        print(f"Error: {e}")
        return None