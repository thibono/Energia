/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  ************************************************************************
  *	MSP430Uploader.java
  *
  *	abstract uploading baseclass for msp430
  *		Copyright (c) 2012 Robert Wessels. All right reserved.
  *
  *
  ***********************************************************************
  Derived from:
  Uploader - abstract uploading baseclass (common to both uisp and avrdude)
  Part of the Arduino project - http://www.arduino.cc/

  Copyright (c) 2004-05
  Hernando Barragan

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  
  $Id$
*/
package processing.app.debug;

import processing.app.Base;
import processing.app.Preferences;
import processing.app.Serial;
import processing.app.SerialException;
import processing.app.SerialNotFoundException;
import processing.app.Editor;
import javax.swing.JOptionPane;


import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;
import gnu.io.*;

public class MSP430Uploader extends Uploader{
	private Editor editor;

	public MSP430Uploader(Editor editor) {
		this.editor = editor;
	}

	public boolean uploadUsingPreferences(String buildPath, String className, boolean usingProgrammer)
	throws RunnerException, SerialException {
		this.verbose = verbose;
		Map<String, String> boardPreferences = Base.getBoardPreferences();
    String protocol = boardPreferences.get("upload.protocol");

    if (protocol.equals("msp430-gdb")) {
      try {
        boolean ret = gdbloader(buildPath, className);
	      if (ret == false) {
		      JOptionPane.showMessageDialog(editor,
              "Unable to upload new firmware to the target board\n"
              + "Please check connections on your target board", "Unable to upload new firmware", JOptionPane.ERROR_MESSAGE);
          return ret;
        }
      } catch (SerialNotFoundException e) {
		      JOptionPane.showMessageDialog(editor,
              "Serial port not found\n", "Unable to upload new firmware", JOptionPane.ERROR_MESSAGE);
          return false;
      }
    }

		Target target = Base.getTarget();
		Collection params = new ArrayList();

		if (Base.isMacOS() || Base.isLinux()) {
			params.add(boardPreferences.get("upload.protocol"));
			if(!Preferences.getBoolean("upload.verbose"))
				params.add("-q");
			params.add("--force-reset");
			if ( Base.isLinux()) {
				params.add("prog " + buildPath + File.separator + className + ".hex");
			} else { 
				params.add("prog " + buildPath + File.separator + className + ".hex");
			}
		} else {
			params.add("tilib");       // always use the TI Lib on Windows, best integrate on this OS
			if(!Preferences.getBoolean("upload.verbose")) params.add("-q");
			params.add("--force-reset");
			params.add("\"prog " + buildPath + File.separator + className + ".hex\"");
		}

		boolean ret = mspdebug(params);
		int retry = 0;
		if(exception == null) return ret;
		if (ret == false && (exception.message.indexOf("update needed") != -1)) {
			JOptionPane.showMessageDialog(editor, 
					"Oops, the firmware of your Launchpad programmer needs an update.\n"
					+ " You will need to update before you can upload your Sketch.\n"
					+ " To update, select \"Update programmer\" from the \"Tools\" menu\n"
					+ "", "Firmware update needed", JOptionPane.ERROR_MESSAGE);
		}

		return ret;
	}

	public boolean doFetUpdate()
		  throws RunnerException, SerialException {

		Collection params = new ArrayList();
		Map<String, String> boardPreferences = Base.getBoardPreferences();

		if (Base.isMacOS() || Base.isLinux()) {
			params.add(boardPreferences.get("upload.protocol"));
		} else {
			params.add("tilib");       // always use the TI Lib on Windows, best integrate on this OS
		}

		params.add("--allow-fw-update");
		params.add("\"exit\"");

		boolean ret = true;
	    ret = mspdebug(params);
		return ret;
	}

	public boolean burnBootloader() throws RunnerException {
		//nothing do do for MSP430
		return false;
	}

	public boolean mspdebug(Collection params) throws RunnerException {
		List commandDownloader = new ArrayList();

		if ( Base.isLinux()) {
			commandDownloader.add(Base.getMSP430BasePath() + "mspdebug"); // tools/msp430/bin or one from PATH
		} 
		else if (Base.isMacOS()) {
			commandDownloader.add(Base.getHardwarePath() + "/tools/msp430/mspdebug/mspdebug");
		}
		else {
			commandDownloader.add(Base.getHardwarePath() + "\\tools\\msp430\\mspdebug\\mspdebug");
		}
		commandDownloader.addAll(params);
		
		return executeUploadCommand(commandDownloader);
	}
	
	public boolean MSP430Flasher(Collection params) throws RunnerException {
		List commandDownloader = new ArrayList();

		commandDownloader.add(Base.toShortPath(Base.getHardwarePath()) + "\\tools\\msp430\\MSP430Flasher\\" + "MSP430Flasher.exe");
		commandDownloader.addAll(params);
		
		return executeUploadCommand(commandDownloader);
	}

	public boolean gdbloader(String buildPath, String className) throws RunnerException, SerialNotFoundException {

    String gdbBin, sysPrompt;
    boolean ret = false;

		if ( Base.isLinux()) {
      gdbBin = Base.getMSP430BasePath() + "msp430-gdb";
      sysPrompt = "/bin/bash";
		}
		else if (Base.isMacOS()) {
			gdbBin = "/tools/msp430/bin/msp430-gdb";
      sysPrompt = "/bin/bash";
		}
		else {
			gdbBin = "\\tools\\msp430\\bin\\msp430-gdb";
      sysPrompt = "cmd";
		}

    try {
      Serial serialPort = new Serial();
      byte[] readBuffer;
      while(serialPort.available() > 0) {
        readBuffer = serialPort.readBytes();
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {}
      }

      // Enable GDB bootloader on target board
      serialPort.setRTS(false);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {}

      // Reset target board
      serialPort.setDTR(false);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {}
      serialPort.setDTR(true);

      String gdbcommand = gdbBin + " -b 38400 " + "-ex 'target remote " + Preferences.get("serial.port") +
      "' -ex 'set debug remote 0' " + buildPath + File.separator + className + ".elf" +
      " -ex 'erase' -ex 'load' -ex 'quit'";
      System.out.println(gdbcommand);
      String line;

      Process process = Runtime.getRuntime().exec(sysPrompt);

      if (process != null) {
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream())),true);

        out.println(gdbcommand);
        out.flush();
        out.close();

        while ((line = in.readLine()) != null) {
            System.out.println(line);
        } 

        process.waitFor();
        process.destroy();

        ret = true;

        serialPort.setRTS(true);
      }

    } catch (SerialNotFoundException e) {
      throw e;
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
      return ret;
    }    
	}
}