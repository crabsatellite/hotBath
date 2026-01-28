"""
Generate fly particle textures for the dirtiness system.
Creates a 2-frame animated fly sprite (8x8 pixels).
"""

from PIL import Image, ImageDraw

def create_fly_texture(frame: int) -> Image.Image:
    """
    Create an 8x8 fly texture.
    Frame 0 = wings up
    Frame 1 = wings down
    """
    img = Image.new('RGBA', (8, 8), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Colors
    body_color = (30, 25, 20, 255)  # Dark brown/black body
    wing_color = (80, 90, 100, 180)  # Semi-transparent grayish wings
    eye_color = (180, 50, 50, 255)  # Reddish eyes
    
    # Body (center, elongated oval) - pixels 3-4 x, 2-5 y
    draw.point((3, 2), body_color)
    draw.point((4, 2), body_color)
    draw.point((3, 3), body_color)
    draw.point((4, 3), body_color)
    draw.point((3, 4), body_color)
    draw.point((4, 4), body_color)
    draw.point((3, 5), body_color)
    draw.point((4, 5), body_color)
    
    # Head (top of body)
    draw.point((3, 1), body_color)
    draw.point((4, 1), body_color)
    
    # Eyes (red dots on head)
    draw.point((2, 1), eye_color)
    draw.point((5, 1), eye_color)
    
    # Wings (change position based on frame for animation)
    if frame == 0:
        # Wings up
        draw.point((1, 2), wing_color)
        draw.point((2, 2), wing_color)
        draw.point((1, 3), wing_color)
        draw.point((2, 3), wing_color)
        
        draw.point((5, 2), wing_color)
        draw.point((6, 2), wing_color)
        draw.point((5, 3), wing_color)
        draw.point((6, 3), wing_color)
    else:
        # Wings down
        draw.point((1, 3), wing_color)
        draw.point((2, 3), wing_color)
        draw.point((1, 4), wing_color)
        draw.point((2, 4), wing_color)
        
        draw.point((5, 3), wing_color)
        draw.point((6, 3), wing_color)
        draw.point((5, 4), wing_color)
        draw.point((6, 4), wing_color)
    
    # Legs (tiny lines at bottom)
    leg_color = (40, 35, 30, 255)
    draw.point((2, 5), leg_color)
    draw.point((5, 5), leg_color)
    draw.point((2, 6), leg_color)
    draw.point((5, 6), leg_color)
    
    return img

def main():
    output_dir = r"e:\MinecraftDev\AlexProject\hotBath\src\main\resources\assets\hotbath\textures\particle"
    
    # Generate 2 frames for wing animation
    for frame in range(2):
        img = create_fly_texture(frame)
        filepath = f"{output_dir}/fly_{frame}.png"
        img.save(filepath)
        print(f"Created: {filepath}")
    
    print("\nFly particle textures generated successfully!")
    print("Frame 0: Wings up")
    print("Frame 1: Wings down")

if __name__ == "__main__":
    main()
