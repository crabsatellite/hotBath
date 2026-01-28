# Dirt Overlay Texture Generator
# Run this script to generate 10 dirt_overlay textures with different patterns
# Requires: pip install Pillow

from PIL import Image, ImageDraw
import random
import os

# Number of different patterns to generate
NUM_PATTERNS = 10

def generate_dirt_overlay(seed, pattern_index):
    """Generate a 64x64 dirt overlay texture matching Minecraft player skin UV layout."""
    
    # Create 64x64 RGBA image (transparent background)
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Dirt color palette (brown/gray tones) - darker colors
    dirt_colors = [
        (54, 40, 30),    # Dark brown
        (72, 50, 38),    # Medium brown
        (87, 55, 28),    # Light brown
        (100, 80, 55),   # Tan
        (60, 60, 60),    # Gray
        (45, 45, 45),    # Dark gray
        (70, 58, 45),    # Brownish gray
        (85, 65, 50),    # Warm brown
    ]
    
    random.seed(seed)  # Different seed for each pattern
    
    # Define UV regions for each body part (x, y, width, height)
    # Based on Minecraft player skin UV layout
    regions = {
        # Legs (most dirt spots)
        'right_leg_front': (4, 20, 4, 12),
        'right_leg_back': (12, 20, 4, 12),
        'right_leg_left': (0, 20, 4, 12),
        'right_leg_right': (8, 20, 4, 12),
        'right_leg_top': (4, 16, 4, 4),
        'right_leg_bottom': (8, 16, 4, 4),
        
        'left_leg_front': (20, 52, 4, 12),
        'left_leg_back': (28, 52, 4, 12),
        'left_leg_left': (16, 52, 4, 12),
        'left_leg_right': (24, 52, 4, 12),
        'left_leg_top': (20, 48, 4, 4),
        'left_leg_bottom': (24, 48, 4, 4),
        
        # Body (moderate dirt)
        'body_front': (20, 20, 8, 12),
        'body_back': (32, 20, 8, 12),
        'body_left': (16, 20, 4, 12),
        'body_right': (28, 20, 4, 12),
        'body_top': (20, 16, 8, 4),
        'body_bottom': (28, 16, 8, 4),
        
        # Arms (moderate dirt)
        'right_arm_front': (44, 20, 4, 12),
        'right_arm_back': (52, 20, 4, 12),
        'right_arm_left': (40, 20, 4, 12),
        'right_arm_right': (48, 20, 4, 12),
        'right_arm_top': (44, 16, 4, 4),
        'right_arm_bottom': (48, 16, 4, 4),
        
        'left_arm_front': (36, 52, 4, 12),
        'left_arm_back': (44, 52, 4, 12),
        'left_arm_left': (32, 52, 4, 12),
        'left_arm_right': (40, 52, 4, 12),
        'left_arm_top': (36, 48, 4, 4),
        'left_arm_bottom': (40, 48, 4, 4),
        
        # Head (least dirt)
        'head_front': (8, 8, 8, 8),
        'head_back': (24, 8, 8, 8),
        'head_left': (0, 8, 8, 8),
        'head_right': (16, 8, 8, 8),
        'head_top': (8, 0, 8, 8),
        'head_bottom': (16, 0, 8, 8),
    }
    
    # Spot density per region type
    density = {
        'leg': 0.25,   # High density
        'body': 0.15,  # Medium density
        'arm': 0.12,   # Medium density
        'head': 0.06,  # Low density
    }
    
    def get_density(region_name):
        if 'leg' in region_name:
            return density['leg']
        elif 'body' in region_name:
            return density['body']
        elif 'arm' in region_name:
            return density['arm']
        elif 'head' in region_name:
            return density['head']
        return density['body']
    
    # Draw dirt spots on each region
    for region_name, (rx, ry, rw, rh) in regions.items():
        spot_density = get_density(region_name)
        num_spots = int(rw * rh * spot_density)
        
        # Bias towards bottom for legs
        bias_bottom = 'leg' in region_name and 'top' not in region_name and 'bottom' not in region_name
        
        for _ in range(num_spots):
            # Random position within region
            if bias_bottom:
                # More spots at bottom (lower part of texture = top of leg)
                x = rx + random.random() * rw
                y = ry + rh * (0.3 + 0.7 * random.random())  # Bias towards bottom
            else:
                x = rx + random.random() * rw
                y = ry + random.random() * rh
            
            # Random spot size (1-2 pixels)
            size = random.choice([1, 1, 1, 2])
            
            # Random color from palette
            color = random.choice(dirt_colors)
            
            # Higher alpha for more visible dirt (220-255)
            alpha = random.randint(220, 255)
            
            # Draw spot
            x1, y1 = int(x), int(y)
            x2, y2 = min(x1 + size, rx + rw), min(y1 + size, ry + rh)
            
            for px in range(x1, x2):
                for py in range(y1, y2):
                    if 0 <= px < 64 and 0 <= py < 64:
                        img.putpixel((px, py), (*color, alpha))
    
    # Save the texture
    output_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), 
                               'src/main/resources/assets/hotbath/textures/entity/player')
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, f'dirt_overlay_{pattern_index}.png')
    img.save(output_path, 'PNG')
    print(f"Generated dirt overlay texture: {output_path}")
    
    return img

def generate_all_patterns():
    """Generate all 10 dirt overlay patterns."""
    print(f"Generating {NUM_PATTERNS} dirt overlay patterns...")
    for i in range(NUM_PATTERNS):
        generate_dirt_overlay(seed=42 + i * 1000, pattern_index=i)
    print("Done!")

if __name__ == '__main__':
    generate_all_patterns()
