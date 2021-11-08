import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class VMWriter {
	
	private BufferedWriter writer;
	
	VMWriter(File file) {
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
		} catch (FileNotFoundException e) {
			System.out.println("Error creating a Writer");
			e.printStackTrace();
		}
	}
	
	void writePush(Segment segment, int index) {
		writeToFile("push " + segmentToString(segment) + " " + index);
	}
	void writePop(Segment segment, int index) {
		writeToFile("pop " + segmentToString(segment) + " " + index);
	}
	void writeArithmetic(Command command){
		writeToFile(command.toString().toLowerCase());
	}
	void writeLabel(String label) {
		writeToFile("label " + label);
	}
	void writeGoto(String label) {
		writeToFile("goto " + label);
	}
	void writeIf(String label) {
		writeToFile("if-goto " + label);
	}
	void writeCall(String name, int nArgs) {
		writeToFile("call " + name + " " + nArgs);
	}
	void writeFunction(String name, int nLocals) {
		writeToFile("function " + name + " " + nLocals);
	}
	void writeReturn() {
		writeToFile("return");
	}
	void close() {
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("Error flushing and closing");
			e.printStackTrace();
		}
	}
	
	void writeToFile(String output) {
		try {
			writer.write(output + '\n');
		} catch (IOException e) {
			System.out.println("Error writing to file");
			e.printStackTrace();
		}
	}
	
	//Helper functions
	private String segmentToString(Segment segToConvert) {
		if(segToConvert == Segment.CONST) {
			return "constant";
		}
		if(segToConvert == Segment.ARG) {
			return "argument";
		}
		return segToConvert.toString().toLowerCase();
	}

}
