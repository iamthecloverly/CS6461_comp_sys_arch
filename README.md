CSCI 6461 - Part 0: Assembler
This project is a two-pass assembler for the C6461 custom instruction set architecture. It is written in Java and can be run from the command line.

How to Compile and Create the JAR File
This project is built using IntelliJ IDEA.

Open the Project: Open the project folder in IntelliJ IDEA.

Configure Artifacts:

Go to File -> Project Structure....

Select Artifacts from the left panel.

Click the + icon, select JAR, and then From modules with dependencies....

For the Main Class, click the folder icon and select the Assembler class.

Ensure 'extract to the target JAR' is selected and click OK.

Build the JAR:

From the main menu, go to Build -> Build Artifacts....

Select your artifact (e.g., CS6461_Assembler:jar) and choose Build.

The final JAR file will be located in out/artifacts/YourProjectName_jar/YourProjectName.jar.

How to Run the Assembler
You must run the assembler from a command-line terminal (like Terminal on macOS).

Navigate to the Directory: Open your terminal and navigate to the folder where the JAR file was created.

cd path/to/your/project/out/artifacts/YourProjectName_jar/

Place Source File: Make sure your assembly source code file (e.g., test_program.txt) is in the same directory as the JAR file.

Execute the Program: Run the assembler by passing the source file name as a command-line argument.

java -jar YourProjectName.jar test_program.txt

(Replace YourProjectName.jar with the actual name of your JAR file).

Outputs
The assembler will create two files in the same directory:

test_program_listing.txt: A detailed listing file showing the original code, memory addresses, and the generated machine code in octal.

test_program_load.txt: A simple two-column file with memory addresses and machine code, ready to be "loaded" by the future simulator.