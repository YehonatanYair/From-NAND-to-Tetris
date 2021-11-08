import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class JackCompiler {
	
	public static void main(String[] args) {
		try {
			File fileOrDirectory = new File(args[0]);
			String path = fileOrDirectory.getAbsolutePath();
			
			//making an array of the files in the directory
			ArrayList<File> files = new ArrayList<>();
			
			//checking if the path provided is a file or a directory, and acting accordingly
			if(fileOrDirectory.isDirectory()) {
				File[] filesArray = fileOrDirectory.listFiles();
		        if (filesArray != null) {
		            for (File jackFile : filesArray) {
		                if (jackFile.getName().endsWith(".jack")) {
		                	files.add(jackFile);
		                } 
		            }
		        }
			}
			else { //file.isFile() == true
				path = path.substring(0,path.length() - fileOrDirectory.getName().length() - 1);
				files.add(fileOrDirectory);
			} 
			
			//going through translation for all files in the folder/going once if the path is a file
			for(File currentFile : files) {
				File fout = new File(path + "/" + currentFile.getName().substring(0,currentFile.getName().length()-5) + ".vm"); 
				SymbolTable symbolTable = new SymbolTable();
				CompilationEngine engine = new CompilationEngine(currentFile,fout,symbolTable);
				engine.compileClass();
				engine.close();
			}
		}
		catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
