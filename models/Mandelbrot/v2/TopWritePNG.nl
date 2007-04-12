

network TopWritePNG (dataFile, imageFile, W, H, maxIter, satLevels) ==> :

	
entities

	reader = ReadFile(fname:: dataFile);
	colorer = SampleColorer(maxIter:: maxIter, satLevels:: satLevels);
	writer = WriteImage(fileName:: imageFile, format:: "PNG", W:: W, H:: H);
	b2w = B2W(bigEndian:: true);
	
structure

	reader.D --> b2w.In;
	b2w.Out --> colorer.N;
		
	colorer.R --> writer.R;
	colorer.G --> writer.G;
	colorer.B --> writer.B;
	
end

