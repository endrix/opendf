
network FileSource (String fileName) ==> int Bytes, boolean Opened :

entities

	fName = InitialTokens(tokens = [fileName]);
	reader = AsynchronousFileReader();
	
structure

	fName.Out --> reader.FileName;
	
	reader.Bytes --> Bytes;
	
	reader.Opened --> Opened;
	
end