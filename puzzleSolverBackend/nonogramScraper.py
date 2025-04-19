import os
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from PIL import Image

# 👇 Function to remove blue border
def remove_blue_border(image):
    pixels = image.load()
    width, height = image.size

    target_rgb = (51, 102, 204)
    tolerance = 15
    background_rgb = (255, 255, 255)

    def is_close_color(rgb1, rgb2, tol):
        return all(abs(c1 - c2) <= tol for c1, c2 in zip(rgb1, rgb2))

    for x in range(width):
        for y in range(height):
            r, g, b = pixels[x, y][:3]
            if is_close_color((r, g, b), target_rgb, tolerance):
                pixels[x, y] = background_rgb
    return image

# Set up headless Chrome
chrome_options = Options()
chrome_options.add_argument('--headless')
chrome_options.add_argument('--disable-gpu')
chrome_options.add_argument('--window-size=1200,1000')

CHROMEDRIVER_PATH = r'C:\WebDrivers\chromedriver-win64\chromedriver.exe'
OUTPUT_DIR = 'nonogram_puzzles'
os.makedirs(OUTPUT_DIR, exist_ok=True)

service = Service(CHROMEDRIVER_PATH)
driver = webdriver.Chrome(service=service, options=chrome_options)

try:
    for i in range(3000):  # Adjust number as needed
        puzzle_urls = [
            ('https://www.puzzle-nonograms.com/', '5x5'),
            ('https://www.puzzle-nonograms.com/?size=1', '10x10')
        ]

        puzzle_url, label = puzzle_urls[i % len(puzzle_urls)]
        driver.get(puzzle_url)
        time.sleep(3)

        screenshot_path = os.path.join(OUTPUT_DIR, 'temp.png')
        driver.save_screenshot(screenshot_path)

        with Image.open(screenshot_path) as img:
            if label == '5x5':
                crop_box = (664, 325, 857, 495)
            else:
                crop_box = (609, 322, 908, 619)

            cropped = img.crop(crop_box)
            cleaned = remove_blue_border(cropped)

            final_path = os.path.join(OUTPUT_DIR, f'nonogram_{label}_{i}.png')
            cleaned.save(final_path)

        os.remove(screenshot_path)  # optional cleanup
        print(f'Saved nonogram puzzle {i + 1}: {label}')

finally:
    driver.quit()
