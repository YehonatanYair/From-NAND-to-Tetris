package vmtranslator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class VMTranslator {
	
	public static void main(String[] args) {
		try {
			File file = new File(args[0]);
			String path = file.getAbsolutePath();
			int pathLength = path.length();
			String fileName;
			File fout;
			//checking if the path provided is a file or a directory, and acting accordingly
			if(file.isDirectory()) {
				fileName = path.substring(path.lastIndexOf('\\') + 1, pathLength);
				path += "\\" + fileName + ".asm";
				fout = new File(path);
			}
			else {
				fileName = path.substring(path.lastIndexOf('\\') + 1, pathLength-2);
				fout = new File(path.substring(0, pathLength-3) + ".asm");
			}
			//making an array of the files in the directory
			ArrayList<File> files = new ArrayList<>();
			CodeWriter codeWriter = new CodeWriter(fout);
			
			if(file.isDirectory()) {
				File[] filesArray = file.listFiles();
		        if (filesArray != null) {
		            for (File vmFile : filesArray) {
		                if (vmFile.getName().endsWith(".vm")) {
		                	files.add(vmFile);
		                } 
		            }
		        }
			}
			else { //file.isFile() == true
				files.add(file);
			} 
			codeWriter.writeInit();
			//going through translation for all files in the folder/going once if the path is a file
			for(File currentFile : files) {
				codeWriter.setFileName(currentFile.getName().substring(0,currentFile.getName().length()-2));
				Parser parser = new Parser(currentFile);
				while(parser.hasMoreCommands()) {
					parser.advance();
					Command currentCommand = parser.commandType();
					if(currentCommand != null) {
						switch (currentCommand) {
						case C_PUSH:
							codeWriter.writePushPop(currentCommand,parser.arg1(),parser.arg2());
							break;
						case C_POP:
							codeWriter.writePushPop(currentCommand,parser.arg1(),parser.arg2());
							break;
						case C_ARITHMETIC:
							codeWriter.writeArithmetic(parser.arg1());
							break;
						case C_LABEL:
							codeWriter.writeLabel(parser.arg1());
							break;
						case C_GOTO:
							codeWriter.writeGoto(parser.arg1());
							break;
						case C_IF:
							codeWriter.writeIf(parser.arg1());
							break;
						case C_FUNCTION:
							codeWriter.writeFunction(parser.arg1(),parser.arg2());
							break;
						case C_CALL:
							codeWriter.writeCall(parser.arg1(),parser.arg2());
							break;
						case C_RETURN:
							codeWriter.writeReturn();
							break;
						default:
							System.out.println("unknown command");
							break;
						}
					}
				}
			}
			codeWriter.close();
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
