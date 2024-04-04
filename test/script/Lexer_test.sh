#!/bin/bash

# Define the relative path to the bin directory where Lexer.class is located
binDir="../../bin"

# Define the output directory relative to the script's current location
outputDir="./outputs"

# Ensure the output directory exists, create it if it doesn't
if [ ! -d "$outputDir" ]; then
    mkdir "$outputDir"
fi

# Check if Lexer.class exists in the bin directory
if [ ! -f "$binDir/Lexer.class" ]; then
    echo "Error: Lexer.class not found in the '$binDir' directory."
    exit 1
fi

# Change to the directory where the test files are located before iterating over them
cd ..
pwd

# Loop through each file in the test directory (now the current directory)
for file in *; do
    # Skip directories and scripts, processing only files
    if [ -f "$file" ] && [ "$file" != "script" ]; then
        # Extract the filename without the path for output purposes
        filename=$(basename -- "$file")
        # Run Lexer with the file, output to the "outputs" directory
        # Note the correction in the path for both the input and output files
        (cd "../bin" && java Lexer "../test/$file" > "../test/script/outputs/${filename}.Lexer.txt")
        echo "Processed $file"
    fi
done

# No need to change back, the script ends here
echo "Done processing all files."
