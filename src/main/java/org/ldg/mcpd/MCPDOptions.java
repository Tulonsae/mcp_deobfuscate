package org.ldg.mcpd;

import com.beust.jcommander.Parameter;
import java.util.List;

public class MCPDOptions {
    @Parameter(names = {"-h", "--help"}, help=true, hidden=true,
               description = "print this message")
    public boolean help = false;

    @Parameter(names = {"-c", "--config"},
               description = "RetroGuard-compatible config file")
    public String config;

    @Parameter(names = {"-i", "--infiles"},
               description = "input class/jar file(s)",
               variableArity = true)
    public List<String> inputs;

    @Parameter(names = {"-o", "--outfiles"},
               description = "output class/jar file(s)",
               variableArity = true)
    public List<String> outputs;

    @Parameter(names = {"-f", "--indir"},
               description = "input files are relative to this directory.")
    public String indir;

    @Parameter(names = {"-d", "--outdir"},
               description = "output files are placed in this directory, " +
                             "with their original name (unless --outfiles)")
    public String outdir;

    @Parameter(names = {"-k", "--keepdirs"},
               description = "input directory structure is kept when using " +
                             "--outdir and not --outfiles " +
                             "This is dangerous and prompts to confirm.")
    public boolean keepdirs = false;

    @Parameter(names = {"--unsafe"},
               description = "when specified with --keepdirs, skips the " +
                             "confirmation prompt.  This is intended for " +
                             "script use only!")
    public boolean unsafe = false;

    @Parameter(names = {"-e", "--exclude"},
               description = "packages to exclude from translation " + 
                             "(e.g. external libraries)",
               variableArity=true)
    public List<String> exclude;

    @Parameter(names = {"-s", "--stored_inheritance"},
               description = "inheritance files (as generated by --inheritance)"
                           + " to use in translation.",
               variableArity=true)
    public List<String> library_files;

    @Parameter(names = {"-n", "--inheritance"},
               description = "outputs an inheritance table to the specified "
                           + "file, for later use by --library_inheritance.")
    public String inheritance_file = null;
}
