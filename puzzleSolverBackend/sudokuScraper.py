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

difficulty = 'easy'
SUDOKU_BASE_URL = 'https://www.sudokuweb.org/'

# Output directory
OUTPUT_DIR = 'sudoku_puzzles'
os.makedirs(OUTPUT_DIR, exist_ok=True)

# Initialize WebDriver
service = Service(CHROMEDRIVER_PATH)
driver = webdriver.Chrome(service=service, options=chrome_options)
i = 870

try:
    for i in range(1580, 3000):  # Number of puzzles

        puzzle_url = f'{SUDOKU_BASE_URL}?difficulty={difficulty}'  
        driver.get(puzzle_url)
        time.sleep(4)  # Allow time for full render

        screenshot_path = os.path.join(OUTPUT_DIR, f'sudoku_full_{i}.png')
        driver.save_screenshot(screenshot_path)

        # Save the full image first
        full_image_path = os.path.join(OUTPUT_DIR, f'full_sudoku_{i}.png')
        os.rename(screenshot_path, full_image_path)  # Rename to save the full screenshot

        with Image.open(full_image_path) as img:
            # Adjust crop box based on the actual puzzle's position on the screen
            crop_box = (148, 324, 508, 685)  
            
            puzzle_img = img.crop(crop_box)
            final_path = os.path.join(OUTPUT_DIR, f'sudoku_puzzle_{i}.png')
            puzzle_img.save(final_path)
        
        os.remove(full_image_path)

        print(f'Saved Sudoku puzzle {i + 1}')

finally:
    driver.quit()
