package com.TheRPGAdventurer.ROTD.common.entity.physicalmodel;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by TGG on 8/08/2019.
 *
 * Used to accumulate errors discovered while parsing DragonVariants
 * Usage:
 * DragonVariantsErrors holds the errors.  It can be used as
 * 1) create a DragonVariantsErrors({optional maximum total message length}
 * 2) Add errors to it using addError()
 * 3) toString() to get a list of all errors
 * 4) getAllErrors() to get a list of all the errors
 * 5) clear() to erase all errors
 * 6) hasErrors() to see whether there are errors or not
 *
 * DragonVariantsException can be used to throw DragonVariantsError up to callers:
 * 1) new DraqonVariantsException (String or DragonVariantsError)
 * 2) getDragonVariantsErrors() to extract the errors, or
 * 3) getMessage() to convert to a list of all errors
 *
 */
public class DragonVariantsException extends IllegalArgumentException {

  public DragonVariantsException(String msg) {
    super();
    dragonVariantsErrors = new DragonVariantsErrors();
    dragonVariantsErrors.addError(msg);
  }

  public DragonVariantsException(DragonVariantsErrors dragonVariantsErrors) {
    super();
    this.dragonVariantsErrors = dragonVariantsErrors;
  }

  public DragonVariantsErrors getDragonVariantsErrors() {return dragonVariantsErrors;}

  public String getMessage() {return dragonVariantsErrors.toString();}

  private DragonVariantsErrors dragonVariantsErrors;

  static public class DragonVariantsErrors {
    public DragonVariantsErrors() {};
    public DragonVariantsErrors(int maxlength) {this.maxlength = maxlength;}

    public void addError(DragonVariantsException dragonVariantsException) {
      allErrors.addAll(dragonVariantsException.getDragonVariantsErrors().getAllErrors());
    }

    public void addError(String message) {
      allErrors.add(message);
    }

    public void addError(IllegalArgumentException iae) {
      if (iae instanceof DragonVariantsException) {
        addError((DragonVariantsException)iae);
      } else {
        allErrors.add(iae.getMessage());
      }
    }

    public void clear() {allErrors.clear();}

    public boolean hasErrors() {return !allErrors.isEmpty();}

    public String toString() {
      StringBuilder retval = new StringBuilder(maxlength);
      boolean prependCarret = false;
      for (String msg : allErrors) {
        if (prependCarret) {
          retval.append("\n");
        }
        prependCarret = true;
        if (retval.length() + msg.length() > maxlength) {
          retval.append("..{more}..");
          return retval.toString();
        }
        retval.append(msg);
      }
      return retval.toString();
    }

    private List<String> getAllErrors() {return allErrors;}

    private List<String> allErrors = new LinkedList<>();
    private int maxlength = 1000; // default
  }
}
