/* 
BEGINCOPYRIGHT X,UC
	
	Copyright (c) 2007, Xilinx Inc.
	Copyright (c) 2003, The Regents of the University of California
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the names of the copyright holders nor the names 
	  of contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT
*/

package net.sf.caltrop.cal.i2.shell;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.Executor;
import net.sf.caltrop.cal.interpreter.ast.Expression;
import net.sf.caltrop.cal.interpreter.ast.Statement;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.environment.AccessLoggingEnvironment;
import net.sf.caltrop.cal.i2.environment.AutoBindEnvironment;
import net.sf.caltrop.cal.i2.environment.CacheEnvironment;
import net.sf.caltrop.cal.i2.environment.ImportEnvironment;

import net.sf.caltrop.cal.i2.platform.DefaultUntypedPlatform;
import net.sf.caltrop.cal.i2.util.CalScriptImportHandler;
import net.sf.caltrop.cal.i2.util.ClassLoadingImportHandler;
import net.sf.caltrop.cal.i2.util.EnvironmentFactoryImportHandler;
import net.sf.caltrop.cal.i2.util.ImportHandler;
import net.sf.caltrop.cal.i2.util.Platform;

import net.sf.caltrop.cal.interpreter.util.NullOutputStream;
import net.sf.caltrop.cal.interpreter.util.SourceReader;


/**
 * Interactive shell that understands Cal expression and statement syntax, and can be made to
 * use any Cal platform.
 * <p>
 * Invocation syntax:<br>
 * <tt>net.sf.caltrop.cal.shell.Shell [-platform <it>platform</it>] file1 file2 ...</tt>
 * <p>
 * Interaction happens on a line-by-line basis, where a logical "line" may be continued on the next
 * physical line if the last non-whitespace character on the current line is a backslash "\". The shell understands
 * two kinds of lines: (1) commands, (2) statement sequences.
 * <p>
 * <b>Commands.</b> Commands are prefixed by the <tt>@</tt> character. They are used to control the operation
 * of the shell, and to configure the shell.
 * <dl>
 * <dt><tt>@ <i>expr</i></tt>
 * <dd>Evaluate the specified expression and print the result.
 *
 * <dt><tt>@env</tt>
 * <dd> Prints the environment of user-defined global variables.
 *
 * <dt><tt>@exit</tt>
 * <dd> Exits the shell.
 *
 * <dt><tt>@reset</tt>
 * <dd> Reinitializaes the shell.
 * *
 * <dt><tt>@load </tt><it>file</it>
 * <dd> Loads the specified file.
 *
 * <dt><tt>@import all </tt><i>package</i>
 * <dd> Import all symbols in the specified package into the global namespace.
 *
 * <dt><tt>@import </tt><i>class</i><tt> [= </tt><i> name</i><tt>]</tt>
 * <dd> Import the specified class into the global namespace. If no explicit name is given,
 * it is imported under its unqualified class name. Otherwise, it is imported as the specified
 * name.
 *
 * <dt><tt>@debug</tt>
 * <dd>Set shell into debugging mode, which causes more diagnostics to be printed in case of failure.
 *
 * <dt><tt>@nodebug</tt>
 * <dd> Set shell into normal mode, turning off debugging messages.
 * </dl>
 * <p>
 * <b>Statement sequences.</b> Any input line that does not start with a <tt>@</tt> character is interpreted
 * as a Cal statement sequence, and executed accordingly. Any modification of the
 * shell state through assignments to global variables is registered and reported after the statements
 * have been executed.
 *
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 *
 * $Id: Shell.java 52 2007-01-22 15:56:51Z imiller $
 */

public class Shell {


    /**
     * Instatiates a shell running the specified platform and using the specified bindings in its initial
     * environment.
     * <p>
     * The streams define the input, output, and error streams for the shell. If the <tt>interactive</tt>
     * parameter is <tt>true</tt>, the shell will prompt for each input and print each result. Otherwise it
     * will be silent, except for error messages.
     *
     * @param platform The platform.
     * @param bindings The bindings to be put into the global environment initially.
     * @param in The input stream for commands and statements.
     * @param out The output stream for prompts, results, etc.
     * @param err The output streams for error messages.
     * @param interactive If true, prompts and results will be printed.
     */

    public Shell(Platform platform, Map bindings,
                 InputStream in, OutputStream out, OutputStream err,
                 boolean interactive) {

        this.platform = platform;
        inputReader = new BufferedReader(new InputStreamReader(in));
        outputWriter = new PrintWriter(out, true);
        errorWriter = new PrintWriter(err, true);
        this.interactive = interactive;

        initializeShellState();
        addBindings(bindings);
        generateImportHandlers();
    }

    /**
     * Executes the shell over the entire input stream, terminating only once the input stream has reached
     * its EOF. The return value will be all the global bindings after the execution of the last input line.
     *
     * @return The final global bindings.
     * @throws IOException
     */

    public Map executeAll() throws IOException {
        while (true) {
            if (execute1())
                return shellStateEnv.localBindings();
        }
    }

    /**
     * Executes the next input line. Returns <tt>true</tt> if it has reached its EOF.
     *
     * @return True if EOF has been reached.
     * @throws IOException
     */

    public boolean execute1() throws IOException {
        String s = collectInput();

        if (s == null)
            return true;

        s = s.trim();
        if (s.length() > 0) {
            if (s.charAt(0) == commandChar) {
                processCommand(s);
            } else {
                interpretStatements(s);
            }
        }
        return false;
    }

    /**
     * Get the global bindings of this shell.
     *
     * @return The global bindings of this shell.
     */
    public Map  bindings() {
        return shellStateEnv.localBindings();
    }

    //
    //  private
    //

    private String collectInput() throws IOException {

        if (interactive) {
            outputWriter.write(promptFirst);
            outputWriter.flush();
        }
        String s = inputReader.readLine();
        if (s == null) {
            return null;
        } else
            s = s.trim();
        boolean readOn = false;
        if (s.length() > 0 && s.charAt(s.length() - 1) == continuationChar) {
            s = s.substring(0, s.length() - 1) + "\n";
            readOn = true;
        }
        StringBuffer inputLine = new StringBuffer(s);
        while (readOn) {
            if (interactive) {
                outputWriter.write(promptContinue);
                outputWriter.flush();
            }
            s = inputReader.readLine();
            if (s == null) {
                readOn = false;
            } else {
                s = s.trim();
                readOn = false;
                if (s.length() > 0 && s.charAt(s.length() - 1) == continuationChar) {
                    s = s.substring(0, s.length() - 1) + "\n";
                    readOn = true;
                }
                inputLine.append(s);
            }
        }

        return inputLine.toString();
    }


    private void  interpretStatements(String src) throws IOException {

        Statement [] s;

        try {
            s = SourceReader.readStmt(src);
        } catch (Exception e) {
            errorWriter.println("error: " + e.getMessage());
            if (shellDebug) {
            	e.printStackTrace(errorWriter); 
            }
            return;
        }

        loggingEnv.clearLogs();

        Executor exec = new Executor(configuration, loggingEnv);
        for (int j = 0; j < s.length; j++) {
            try {
            	int ss = exec.size();
                exec.execute(s[j]);
                if (ss != exec.size()) {
                	errorWriter.println("WARNING: Inconsistent stack. (From " + ss + " to " + exec.size() + ".)");
                }
            } catch (Exception e) {
                errorWriter.println("error at [" + j + "]: " + e.getMessage());
                if (shellDebug)
                    e.printStackTrace(errorWriter);
            }
        }

        Set v = loggingEnv.loggedVars();
        List recs = new ArrayList();
        for (Iterator vars = v.iterator(); vars.hasNext(); ) {
            Object var = vars.next();
            AccessLoggingEnvironment.AccessRecord rec = loggingEnv.getLog(var).getLastRecord();
            insertAccessRecordIntoList(recs, rec);
        }

        for (Iterator i = recs.iterator(); i.hasNext(); ) {
            AccessLoggingEnvironment.AccessRecord rec = (AccessLoggingEnvironment.AccessRecord)i.next();
            outputWriter.write(rec.name + " <- " + rec.value + "\n");
        }
    }

    private void  initializeShellState() {
        if (platform == null)
            platform = new DefaultUntypedPlatform();

        configuration = platform.configuration();

        importEnv = new ImportEnvironment(configuration, Shell.class.getClassLoader());
        cacheEnv = new CacheEnvironment(importEnv);
        globalEnv = platform.createGlobalEnvironment(cacheEnv);
        shellStateEnv = new AutoBindEnvironment(globalEnv);
        loggingEnv = new AccessLoggingEnvironment(shellStateEnv);
    }


    private void  insertAccessRecordIntoList(List recs, AccessLoggingEnvironment.AccessRecord rec) {

        int i = 0;
        while (i < recs.size()) {
            AccessLoggingEnvironment.AccessRecord r = (AccessLoggingEnvironment.AccessRecord)recs.get(i);
            if (rec.eventNumber < r.eventNumber) {
                recs.add(i, rec);
                return;
            }
            i += 1;
        }
        recs.add(rec);
    }

    private void  addBindings(Map m) {

        for (Iterator i = m.keySet().iterator(); i.hasNext(); ) {
            Object k = i.next();
            shellStateEnv.setByName(k, m.get(k));
        }
    }

    private void  processCommand(String s) {
        for (Iterator i = shellCommands.keySet().iterator(); i.hasNext(); ) {
            String cmd = (String)i.next();

            String commandPrefix = Character.toString(commandChar) + cmd;
            if (s.equals(commandPrefix) || s.startsWith(commandPrefix + " ")) {
                String commandArg = s.substring(commandPrefix.length());
                Command command = (Command)shellCommands.get(cmd);
                try {
                    command.execute(commandArg);
                } catch (Exception e) {
                    errorWriter.println("error: " + e.getMessage());
                    if (shellDebug)
                        e.printStackTrace(errorWriter);
                }
                return;
            }
        }
        errorWriter.println("error: Unknown command '" + s + "'.");
    }



    private Platform     platform = null;

    private Configuration            configuration;
    private ImportEnvironment        importEnv;
    private CacheEnvironment         cacheEnv;
    private Environment              globalEnv;
    private AutoBindEnvironment      shellStateEnv;
    private AccessLoggingEnvironment loggingEnv;

    private PrintWriter  outputWriter;
    private PrintWriter  errorWriter;
    private BufferedReader inputReader;

    private boolean      shellDebug = false;
    private boolean      shellDebug0 = false;
    private boolean      interactive;


    private static final char       continuationChar = '\\';
    private static final char       commandChar = '@';
    private static final String     promptFirst = "-->";
    private static final String     promptContinue = "..>";


    private Map  shellCommands = new HashMap();

    {

        shellCommands.put("", new Command () {

            public void execute(String s) throws Exception {
                Evaluator eval = new Evaluator(shellStateEnv, configuration);
                Expression e = SourceReader.readExpr(s);

                int ss = eval.size();
                Object value = eval.valueOf(e);
                outputWriter.println("result: " + value);
                if (ss != eval.size()) {
                	errorWriter.println("WARNING: Inconsistent stack. (From " + ss + " to " + eval.size() + ".)");
                }
            }
        });

        shellCommands.put("env", new Command() {

            public void execute(String s) throws Exception {
                for (Iterator i = shellStateEnv.localBindings().keySet().iterator(); i.hasNext(); ) {
                    Object var = i.next();
                    outputWriter.write(var + " <- " + shellStateEnv.getByName(var) + "\n");
                }
            }
        });

        shellCommands.put("exit", new Command() {

            public void execute (String s) throws Exception {
                System.exit(0);
            }

        });

        shellCommands.put("reset", new Command() {

            public void execute(String s) throws Exception {
                String platformName = s.trim();
                if (!"".equals(platformName)) {
                    platform = getPlatform(platformName);
                }
                initializeShellState();
            }
        });

        shellCommands.put("load", new Command() {

            public void execute (String s) throws Exception {
                String fileName = s.trim();
                Map m = loadFile(fileName, Collections.EMPTY_MAP);
                addBindings(m);
            }
        });

        shellCommands.put("import", new Command() {

            //FIXME: this version does not do *any* syntax checking.
        	// FIXME: use import handlers

            public void  execute(String s) throws Exception {

                String s1 = s.trim();
                if (s1.startsWith("all ")) {
                    String packagePrefix = s1.substring(4).trim();
                    importEnv.importPackage(packagePrefix);
                } else {
                    String variable = null;
                    String qualClassName;
                    int eqPos = s1.indexOf('=');
                    if (eqPos >= 0) {
                        qualClassName = s1.substring(0, eqPos).trim();
                        variable = s1.substring(eqPos + 1).trim();
                    } else {
                        qualClassName = s1;
                    }

                    int dotPos = qualClassName.lastIndexOf('.');
                    if (dotPos < 0)
                        throw new RuntimeException("Qualified class name must contain '.' character.");

                    String packagePrefix = qualClassName.substring(0, dotPos).trim();
                    String className = qualClassName.substring(dotPos + 1).trim();

                    if (variable == null)
                        importEnv.importClass(packagePrefix, className);
                    else
                        importEnv.importClass(packagePrefix, className, variable);
                }
            }
        });

        shellCommands.put("imports", new Command() {

            public void execute(String s) throws Exception {

                boolean first = true;
                for (Iterator i = importEnv.importedClasses().iterator(); i.hasNext(); ) {
                    if (first) {
                        first = false;
                        outputWriter.write("Classes: \n");
                    }
                    String alias = (String)i.next();
                    String name = importEnv.importedClassName(alias);
                    String packagePrefix = importEnv.importedClassPackage(alias);
                    outputWriter.write("  " + alias + ": " + packagePrefix + "." + name + "\n");
                }
                first = true;
                for (Iterator i = importEnv.importedPackages().iterator(); i.hasNext(); ) {
                    if (first) {
                        first = false;
                        outputWriter.write("Packages: \n");
                    }
                    String packageName = (String)i.next();
                    outputWriter.write("  " + packageName + "\n");
                }
            }
        });

        shellCommands.put("debug", new Command() {

            public void execute(String s) { shellDebug = true; }

        });

        shellCommands.put("debug0", new Command() {

            public void execute(String s) { shellDebug = true; shellDebug0 = true; }

        });

        shellCommands.put("nodebug", new Command() {

            public void execute(String s) { shellDebug = false; shellDebug0 = false; }

        });


    }

    //////////////////////////////////////////////////////////////////////////////////
    //////  STATIC
    //////////////////////////////////////////////////////////////////////////////////


    public static void main (String [] args) throws IOException {

        try {
            processArguments(args);
        } catch (Exception e) {
            System.err.println("incorrect arguments: " + e.getMessage());
            printUsage();
            return;
        }
        
        if (verbose) {
        	System.out.println("Using platform '" + defaultPlatform.getClass().getName() + "'.");
        }
        
        Map bindings = loadFiles();

        new Shell(defaultPlatform, bindings, System.in, System.out, System.err, true).executeAll();
    }

    private static Map  loadFiles() {

        Map bindings = Collections.EMPTY_MAP;

        for (Iterator i = files.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            bindings = loadFile(name, bindings);
        }
        return bindings;
    }

    private static Map  loadFile(String filename, Map bindings) {
        System.out.println("Loading file: '" + filename + "'...");
        InputStream in = null;
        try {
            in = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            System.err.println("error: Cannot find file '" + filename + "'.");
        }

        Shell s = new Shell(defaultPlatform, bindings, in, NullOutputStream.devNull, System.err, false);

        try {
            return s.executeAll();
        } catch (IOException e) {
            System.err.println("error: IO error while reading from file '" + filename + "'.");
            return bindings;
        }
    }


    private static Platform getPlatform(String s) {
        try {
            Class platformClass = Class.forName(s);
            return (Platform)platformClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot locate platform class '" + s + "'.");
        } catch (ClassCastException e) {
            throw new RuntimeException("Class '" + s + "' not a valid platform class.");
        } catch (InstantiationException e) {
            throw new RuntimeException("Cannot instantiate platform class '" + s + "'.");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot instantiate platform class '" + s + "'.");
        }
    }


    private static void  printUsage() {
        System.err.println("net.sf.caltrop.cal.shell.Shell [-platform <platformClass>] files ...");
    }



    private static void  processArguments(String [] args) throws Exception {

        ArgumentHandler defaultHandler = (ArgumentHandler)argumentHandlers.get("");

        for (int i = 0; i < args.length; ) {
            if ((!"".equals(args[i])) && argumentHandlers.keySet().contains(args[i])) {
                ArgumentHandler handler = (ArgumentHandler)argumentHandlers.get(args[i]);
                i = processArgument(args, i + 1, handler);
            } else {
                i = processArgument(args, i, defaultHandler);
            }
        }

    }

    private static int  processArgument(String [] args, int i, ArgumentHandler handler) throws Exception {
        int n = handler.arity();
        if (i + n > args.length)
            throw new RuntimeException("Too few arguments for option.");
        String [] a = new String [n];
        for (int j = 0; j < n; j++)
            a[j] = args[i + j];
        handler.action(a);
        return i + n;
    }


    private static List         files = new ArrayList();
    private static Platform     defaultPlatform;
    private static boolean      verbose = false;

    private static Map  argumentHandlers = new HashMap();

    static {
        argumentHandlers.put("", new ArgumentHandler() {
            public void  action(String [] args) {
                files.add(args[0]);
            }

            public int  arity() { return 1; }
        });

        argumentHandlers.put("-verbose", new ArgumentHandler() {
            public void  action(String [] args) {
            	verbose = true;
            }

            public int  arity() { return 0; }
        });

        argumentHandlers.put("-platform", new ArgumentHandler() {
           public void  action(String [] args) throws Exception {
               if (defaultPlatform != null)
                   throw new RuntimeException("Duplicate -platform option.");

               defaultPlatform = getPlatform(args[0]);
           }

           public int  arity() { return 1; }
        });
    }


    /**
     * Implementors of this interface are realizing shell commands. The string argument is the remainder of the
     * command line, after stripping off the command character and the command name.
     */
    interface Command {
        void execute(String s) throws Exception;
    }

    /**
     * Implementors of this interface are processing command-line arguments. They identify how many of the
     * arguments they will be processing, and are handed over exactly this number of arguments in their
     * <tt>action(...)</tt> method.
     */
    interface ArgumentHandler {
        void action(String [] args) throws Exception;

        int  arity();
    }

    private ImportHandler [] importHandlers;
    
    private void generateImportHandlers () {
    	importHandlers = new ImportHandler [] {
    			new EnvironmentFactoryImportHandler(platform),
				new CalScriptImportHandler(platform),
				new ClassLoadingImportHandler(platform, 
    					                      Shell.class.getClassLoader())};
    }    

}
