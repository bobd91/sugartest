/**
 * 
 */
package org.sugarj.test.listener.helper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.ui.PlatformUI;
import org.strategoxt.imp.testing.listener.ITestListener;

/**
 * Copy of org.strategoxt.imp.testing.listener.helper.ListenerWrapper
 * as it is not exported from Spoofax-Testing plugin
 *
 */
public final class ListenerWrapper implements ITestListener {

  private static ITestListener instance;

  public static ITestListener instance() throws CoreException {
    if (instance == null)
      instance = new ListenerWrapper();

    return instance;
  }

  private ListenerWrapper() {
  }

  private Object getWrapped() throws CoreException {
    IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
        ITestListener.EXTENSION_ID);

    Object candidateListener = null;
    String preferredView = PlatformUI.getPreferenceStore().getString(PreferenceConstants.P_LISTENER_ID);
    if (preferredView.equals(""))
      preferredView = PreferenceInitializer.DEFAULT_LISTENER_ID;

    for (IConfigurationElement e : config) {
      if (((RegistryContributor) e.getContributor()).getActualName().equals(preferredView)) {
        candidateListener = e.createExecutableExtension("class");
        break;
      }
    }

    return candidateListener;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.strategoxt.imp.testing.listener.ITestListener#reset()
   */
  public void reset() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
      IllegalAccessException, InvocationTargetException, CoreException {

    Object wrapped = getWrapped();
    // Using reflection, because if I use a cast, I get a ClassCastException
    // cannot cast type <x> to <x>. Probably because of some different classloader issue.
    Method m = wrapped.getClass().getMethod("reset", new Class[] {});
    if (!Modifier.isAbstract(m.getModifiers())) {
      m.invoke(wrapped);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.strategoxt.imp.testing.listener.ITestListener#addTestcase(java.lang.String, java.lang.String, int)
   */
  public void addTestcase(String testsuite, String description, int offset) throws IllegalArgumentException,
      IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException, CoreException {

    Object wrapped = getWrapped();
    Method m = wrapped.getClass().getMethod("addTestcase", new Class[] { String.class, String.class, int.class });
    if (!Modifier.isAbstract(m.getModifiers())) {
      m.invoke(wrapped, testsuite, description, offset);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.strategoxt.imp.testing.listener.ITestListener#addTestsuite(java.lang.String, java.lang.String)
   */
  public void addTestsuite(String name, String filename) throws IllegalArgumentException, IllegalAccessException,
      InvocationTargetException, SecurityException, NoSuchMethodException, CoreException {

    Object wrapped = getWrapped();
    Method m = wrapped.getClass().getMethod("addTestsuite", new Class[] { String.class, String.class });
    if (!Modifier.isAbstract(m.getModifiers())) {
      m.invoke(wrapped, name, filename);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.strategoxt.imp.testing.listener.ITestListener#startTestcase(java.lang.String, java.lang.String)
   */
  public void startTestcase(String testsuite, String description) throws SecurityException, NoSuchMethodException,
      IllegalArgumentException, IllegalAccessException, InvocationTargetException, CoreException {

    Object wrapped = getWrapped();
    Method m = wrapped.getClass().getMethod("startTestcase", new Class[] { String.class, String.class });
    if (!Modifier.isAbstract(m.getModifiers())) {
      m.invoke(wrapped, testsuite, description);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.strategoxt.imp.testing.listener.ITestListener#finishTestcase(java.lang.String, java.lang.String,
   * boolean)
   */
  public void finishTestcase(String testsuite, String description, boolean succeeded)
      throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException,
      NoSuchMethodException, CoreException {

    Object wrapped = getWrapped();
    Method m = wrapped.getClass().getMethod("finishTestcase",
        new Class[] { String.class, String.class, boolean.class });
    if (!Modifier.isAbstract(m.getModifiers())) {
      m.invoke(wrapped, testsuite, description, succeeded);

    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.strategoxt.imp.testing.listener.ITestListener#disableRefresh()
   */
  public void disableRefresh() {
    // the test provider doesn't use this hack
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.strategoxt.imp.testing.listener.ITestListener#enableRefresh()
   */
  public void enableRefresh() {
    // the test provider doesn't use this hack
  }

  /**
   * Copy of org.strategoxt.imp.testing.preferences.PreferenceInitialiser
   * as it is not exported from Spoofax Testing plugin
   *
   */
  static class PreferenceInitializer {
    public final static String DEFAULT_LISTENER_ID = "org.strategoxt.imp.testing.ui";
  }

  /**
   * Copy of org.strategoxt.imp.testing.preferences.PreferenceConstants
   * as it is not exported from Spoofax Testing plugin
   *
   */
  static class PreferenceConstants {
    public static final String P_LISTENER_ID = "listener.id";
  }

}
