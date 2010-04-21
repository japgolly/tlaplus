package org.lamport.tla.toolbox.tool.prover.job;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.lamport.tla.toolbox.tool.prover.ProverActivator;
import org.lamport.tla.toolbox.tool.prover.output.IProverProcessOutputSink;
import org.lamport.tla.toolbox.tool.prover.output.internal.BroadcastStreamListener;

public class ProverJob extends Job
{

    /**
     * the IPath pointing to the module to be checked, e.g.
     *    new Path("C:/Users/drickett/work/svn-repository/examples/HourClock/HourClock.tla")
     */
    private IPath modulePath;
    /**
     * Path to the tlapm executable or null.
     * 
     * Null if it is assumed
     * to be in the system Path
     */
    private IPath tlapmPath;
    /**
     * Path to the folder containing cygwin or null.
     * 
     * Null if this is not Windows or the cygwin path
     * is assumed to be in the System Path.
     */
    private IPath cygwinPath;
    /**
     * The {@link ILaunch} that resulted in the
     * creation of this job.
     */
    private ILaunch launch;
    /**
     * The process for the prover.
     */
    private IProcess proverProcess;
    /**
     * Broadcasts prover output
     * to registered listeners.
     */
    private BroadcastStreamListener listener;
    protected static final long TIMEOUT = 1000 * 1;

    /**
     * Constructor.
     * 
     * @param name human readable name for the job, will appear in progress monitor
     * @param module the IPath pointing to the module to be checked, e.g.
     *    new Path("C:/Users/drickett/work/svn-repository/examples/HourClock/HourClock.tla")
     * @param tlapmPath absolute path to the tlapm executable, or null if it is assumed
     * to be in the system Path
     * @param cygwinPath absolute path to the folder containing cygwin, or null
     * if this is not Windows or the cygwin path is assumed to be in the System Path.
     */
    public ProverJob(String name, IPath modulePath, IPath tlapmPath, IPath cygwinPath, ILaunch launch)
    {
        super(name);
        this.modulePath = modulePath;
        this.tlapmPath = tlapmPath;
        this.cygwinPath = cygwinPath;
        this.launch = launch;
    }

    protected IStatus run(IProgressMonitor monitor)
    {
        try
        {
            /*
             * Check that the module exists and that the tlapm
             * and cygwin paths are valid paths.
             */

            if (!modulePath.toFile().exists())
            {
                ProverActivator.logDebug("Module file given to ProverJob does not exist.");
                return new Status(IStatus.ERROR, ProverActivator.PLUGIN_ID, "Module file does not exist.");
            } else if (tlapmPath != null && !tlapmPath.toFile().exists())
            {
                ProverActivator.logDebug("The given tlapm path does not exist.");
                // TODO show error message to user
                return new Status(IStatus.ERROR, ProverActivator.PLUGIN_ID, "The given tlapm path does not exist.");
            } else if (cygwinPath != null && !cygwinPath.toFile().exists())
            {
                // TODO show error message to user
                ProverActivator.logDebug("The given cygwin path does not exist.");
                return new Status(IStatus.ERROR, ProverActivator.PLUGIN_ID, "The given cygwin path does not exist.");
            }

            /*
             * Launch the prover.
             * 
             * For all operating systems, we need to know where the prover
             * is installed or assume that the user has set the PATH environmental
             * variable to include the path to the prover.
             * 
             * This will be operating system dependent. On Windows,
             * the prover requires Cygwin. According to a post
             * on http://stackoverflow.com/questions/1307202/how-can-i-run-cygwin-from-java,
             * 

            If you are trying to run a binary that requires the cygwin1.dll
            (which includes most commands you can execute from the cygwin bash shell)
            then you can run it by specifying the cygwin\bin directory in the path environment variable like this:

            Process p = Runtime.getRuntime().exec(
            "C:/path/to/cygwin/binary.exe", new String[] { "PATH=C:\\cygwin\\bin" });
            
            This assumes you installed cygwin in C:\cygwin
            
             * This requires knowing where cygwin is installed. We could make this something
             * that can be set in preferences. Another idea is to tell
             * the user to set the PATH variable himself.
             * 
             * On Linux and Mac, I don't think anything extra needs to be done.
             * 
             * I think that the working directory should be set to the one containing
             * the module.
             */

            /*
             * Launch from the command line:
             * 
             * > <tlapm-command> -C moduleName
             * 
             * If no path has been specified (probably in the preferences
             * by the user, then we assume the path to the tlapm has been
             * put in the system Path, and <tlapm-command> is tlapm. If a path
             * has been specified, <tlapm-command> is the path to the tlapm
             * executable.
             * 
             * Module name should end with .tla
             * such as HourClock.tla
             */
            String tlapmCommand = "tlapm";
            if (tlapmPath != null)
            {
                tlapmCommand = tlapmPath.toOSString();
            }
            ProcessBuilder pb = new ProcessBuilder(new String[] { tlapmCommand, "-C", "-k", "--paranoid",
                    modulePath.lastSegment() });

            /*
             * Set the working directory to be the directory
             * containing the module.
             */
            pb.directory(modulePath.toFile().getParentFile());

            /*
             * Add the cygwin directory to the path variable for Windows OS.
             * If the path to cygwin is not given, we assume that it has already been placed
             * in the system Path.
             * 
             * Note that Platform.OS_WIN32 is the only constant for Windows
             * operating systems. The documentation says that it is for
             * 32-bit windows operating systems, but hopefully it also is
             * for 64-bit systems. This needs to be tested.
             */
            if (Platform.isRunning() && Platform.getOS().equals(Platform.OS_WIN32) && cygwinPath != null)
            {
                String pathVar = "Path";
                pb.environment().put(pathVar, pb.environment().get(pathVar) + ";" + cygwinPath.toOSString());
            }

            System.out.println(pb.environment().get("Path"));

            pb.redirectErrorStream(true);

            monitor.beginTask("Running prover.", IProgressMonitor.UNKNOWN);

            /*
             * Start the process. Calling DebugPlugin.newProcess()
             * wraps the java.lang.Process in an IProcess with some
             * convenience methods.
             */
            proverProcess = DebugPlugin.newProcess(launch, pb.start(), getName());

            if (proverProcess != null)
            {
                /*
                 * Setup the broadcasting of the prover output stream.
                 * 
                 */
                listener = new BroadcastStreamListener(modulePath.lastSegment(), IProverProcessOutputSink.TYPE_OUT);

                proverProcess.getStreamsProxy().getErrorStreamMonitor().addListener(listener);
                proverProcess.getStreamsProxy().getOutputStreamMonitor().addListener(listener);

                /*
                 * The following loop checks for job cancellation while
                 * the process is running and terminates the process
                 * if the job is canceled while the process is still running.
                 * 
                 * It handles any errors in terminating the process.
                 */
                while (checkAndSleep())
                {
                    // check the cancellation status
                    if (monitor.isCanceled())
                    {
                        // cancel the TLC
                        try
                        {
                            proverProcess.terminate();
                        } catch (DebugException e)
                        {
                            // react on the status code
                            switch (e.getStatus().getCode()) {
                            case DebugException.TARGET_REQUEST_FAILED:
                            case DebugException.NOT_SUPPORTED:
                            default:
                                return new Status(IStatus.ERROR, ProverActivator.PLUGIN_ID,
                                        "Error terminating the running tlapm instance. This is a bug. Make sure to exit the toolbox.");
                            }
                        }

                        // cancellation termination
                        return Status.CANCEL_STATUS;
                    }
                }

                /*
                 * Check for and handle unsuccessful termination that does not cause an exception
                 * to be thrown. The only cause that I am aware of is not having
                 * the path to cygwin in the system environment path on Windows.
                 */
                try
                {
                    if (proverProcess.isTerminated() && proverProcess.getExitValue() != 0
                            && proverProcess.getExitValue() != 1)
                    {
                        return new Status(
                                IStatus.ERROR,
                                ProverActivator.PLUGIN_ID,
                                "Error running tlapm. If this "
                                        + "is Windows, make sure the path to cygwin is in the system path or that the path "
                                        + "to cygwin is specified in the prover preference page. If this does not solve the problem "
                                        + "then report a bug with the error code to the developers at http://bugzilla.tlaplus.net/."
                                        + "\n \n Error code: " + proverProcess.getExitValue());
                    }
                } catch (DebugException e)
                {
                    return new Status(IStatus.ERROR, ProverActivator.PLUGIN_ID,
                            "Error getting exit code for tlapm process. This is a bug. Report it to the developers at http://bugzilla.tlaplus.net/");
                }

                // successful termination
                return Status.OK_STATUS;

            } else
            {
                return new Status(IStatus.ERROR, ProverActivator.PLUGIN_ID,
                        "Error launching prover. Launching the prover returned a null process.");
            }

        } catch (IOException e)
        {
            /*
             * Handle errors properly.
             * 
             * This should definitely show an error to the user
             * if the tlapm executable is not found or if this is
             * Windows and tlapm crashes because cygwin is not found.
             * 
             * Returning an error status shows a message to the user.
             * We may decide that we want to customize the appearance of the
             * message in which case we would not return a status, but instead
             * we would probably use the MessageDialog class.
             */
            return new Status(
                    IStatus.ERROR,
                    ProverActivator.PLUGIN_ID,
                    e.getMessage()
                            + "\n The following could resolve this issue: \n"
                            + "1.) If you did not specify in preferences the path to the tlapm executable, make sure it is in your system path.\n"
                            + "2.) If you specified the absolute path to the tlapm executable in preferences, verify that it is correct.",
                    e);

        } finally
        {
            // send the notification about completion
            if (listener != null)
            {
                listener.streamClosed();
            }
            // make sure to complete the monitor
            monitor.done();
        }
    }

    /**
     * Checks if tlapm is still running.
     * @return true, if tlapm is still running
     */
    public boolean checkAndSleep()
    {
        try
        {
            // go sleep
            Thread.sleep(TIMEOUT);

        } catch (InterruptedException e)
        {
            // nothing to do
            // e.printStackTrace();
        }
        // return true if tlapm is still running
        return (!proverProcess.isTerminated());
    }

    public static void main(String[] args)
    {
        // System.out.println(System.getenv("PATH"));
        // System.out.println(System.getenv("Path"));
        // ProverJob job = new ProverJob("ProverJob Test", new Path(
        // "C:/Users/drickett/work/svn-repository/examples/HourClock/HourClock.tla"), new Path(
        // "C:/cygwin/usr/local/bin/tlapm"), new Path("C:/cygwin/bin"));
        // job.run(new NullProgressMonitor());
    }

}
