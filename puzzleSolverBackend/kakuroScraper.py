import os
import time
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from PIL import Image

# Configure headless Chrome
chrome_options = Options()
chrome_options.add_argument('--headless')
chrome_options.add_argument('--disable-gpu')
chrome_options.add_argument('--window-size=1200,800')

# Path to ChromeDriver
CHROMEDRIVER_PATH = r'C:\WebDrivers\chromedriver-win64\chromedriver.exe'

# Puzzle sizes and difficulties
puzzle_sizes = ['8x8']
difficulty = 'easy'
PUZZLE_BASE_URL = 'https://www.kakuroconquest.com/'

# Output directory
OUTPUT_DIR = 'kakuro_puzzles'
os.makedirs(OUTPUT_DIR, exist_ok=True)

# Initialize WebDriver
service = Service(CHROMEDRIVER_PATH)
driver = webdriver.Chrome(service=service, options=chrome_options)

try:
    for size in puzzle_sizes:
        for i in range(460, 1000):  # Number of puzzles per size
            puzzle_url = f'{PUZZLE_BASE_URL}{size}/{difficulty}'
            driver.get(puzzle_url)
            time.sleep(4)  # Allow time for full render

            screenshot_path = os.path.join(OUTPUT_DIR, f'{size}_full_{i}.png')
            driver.save_screenshot(screenshot_path)

            with Image.open(screenshot_path) as img:
                # Customize crop area as needed for different puzzle sizes
                crop_box = {
                    '8x8': (490, 7, 1000, 519)
                }[size]

                puzzle_img = img.crop(crop_box)
                puzzle_img.save(os.path.join(OUTPUT_DIR, f'{size}_puzzle_{i}.png'))

            print(f'Saved {size} puzzle {i + 1}')
finally:
    driver.quit()
