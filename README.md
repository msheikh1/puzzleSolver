# PuzzleSolver - README
## Overview
An AI-powered mobile app that uses image processing and machine learning to recognize and solve various types of logic puzzles from photos or screenshots.

## This README provides instructions on how to set up and launch the app.

There are 3 different programs to run The App, The Web Scraper, The Model. The App is the main program, the other two can be ran for testing purposes.
# The App

## Prerequisites
### Before launching the app, make sure you have the following installed:

* Android Studio (with Kotlin support)
* Java Development Kit (JDK) 
* Android Emulator or a physical Android device for testing
* Getting Started

* Follow the steps below to set up the project and launch it on your device or emulator.

### 1. Clone the Repository
##### If you haven’t already, clone the project repository to your local machine:

```
bash
Copy code
git clone https://github.com/msheikh1/puzzleSolver 
```
### 2. Open the Project in Android Studio
* Launch Android Studio.
* Select Open an Existing Project.
* Navigate to the folder where the repository was cloned and open it in this case it would be folder "app"
### 3. Sync Project with Gradle Files
After opening the project, Android Studio will prompt you to sync the project with Gradle. Click Sync Now to download the necessary dependencies.

### 4. Configure the Android Emulator or Connect a Physical Device
#### Android Emulator:

* Open AVD Manager from the toolbar in Android Studio.
* Create a new virtual device with the desired configuration (recommended: Pixel 3 or similar).
* Select your emulator and click the green Run button.
#### Physical Android Device (Recommended) :

* Enable Developer Mode and USB Debugging on your Android device.
* Connect your device via USB.
* Connect to physical device to internet (highly recommended)
* Android Studio should automatically detect the device. Select it and click the green Run button.
#### 5. Launch the App
Once the app is built, it will launch automatically on the emulator or physical device. The first time would be the longest wait



# The Web Scraper
This will scrape the internet and save the relevant images for the puzzles; Nonogram, Kakuro, Sudoku. The images are already saved in their respective folders and scraped, but for testing purposes can be ran again
## Prerequisites
### Before launching the app, make sure you have the following installed:
* VS Code
* Python
* Python extension on VS Code
```
pip install selenium
pip install pillow
```
Make sure you also have a ChromeDriver installed that matches your installed version of Chrome. You can install it manually or automate this with:
```
pip install webdriver-manager
```
### 2. Open the folder "puzzleSolverBackend"
* Launch VS Code.
* Select Open the folder "puzzleSolverBackend".
* Navigate on the terminal to the folder "puzzleSolverBackend"
### 3. Run the following commands and wait
```
python kakuroScraper.py
python nonogramScraper.py
python sudokuScraper.py
```

# The Model
This will generate the classify files in order to classify the different puzzles into types. The model is already saved on the app but can be ran again for testing purposes
## Prerequisites
### Before launching the app, make sure you have the following:
* VS Code
* Google Colab
* Google Drive
  
### 2. Open the folder "puzzleSolverBackend"
* Locate the folders: kakuro_puzzles, nonogram_puzzles, sudoku_puzzles
* Save the folders onto your google drive (In a new folder called puzzledataset which should be saved in "My Drive")
* Make sure the path matches - '/content/drive/My Drive/puzzledataset'

### 3. Open Google Colab
* Change runtime from CPU to GPU for way faster performance
* Copy the contents of model.py which is in "puzzleSolverBackend" folder into the colab notebook one code block at a time

### 4. Run the Colab Notebook
* Run the colab notebook
* Save the model
* The resulting tflite model can then be used in the app and replaced with the current model







