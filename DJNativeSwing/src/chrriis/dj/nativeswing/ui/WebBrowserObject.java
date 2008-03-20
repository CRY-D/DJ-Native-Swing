/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 * 
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package chrriis.dj.nativeswing.ui;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import chrriis.common.Disposable;
import chrriis.common.Registry;
import chrriis.common.Utils;
import chrriis.common.WebServer;
import chrriis.common.WebServer.HTTPData;
import chrriis.common.WebServer.HTTPRequest;
import chrriis.common.WebServer.WebServerContent;
import chrriis.dj.nativeswing.LocalMessage;
import chrriis.dj.nativeswing.NativeInterfaceHandler;
import chrriis.dj.nativeswing.ui.event.InitializationEvent;
import chrriis.dj.nativeswing.ui.event.InitializationListener;
import chrriis.dj.nativeswing.ui.event.WebBrowserAdapter;
import chrriis.dj.nativeswing.ui.event.WebBrowserEvent;
import chrriis.dj.nativeswing.ui.event.WebBrowserListener;

/**
 * A helper class to simplify the development of native components that act as a plugin to the web browser component (like the JFlashPlayer).
 * @author Christopher Deckers
 */
public abstract class WebBrowserObject implements Disposable {

  private static class NWebBrowserListener extends WebBrowserAdapter {
    protected Reference<WebBrowserObject> webBrowserObject;
    protected NWebBrowserListener(WebBrowserObject webBrowserObject) {
      this.webBrowserObject = new WeakReference<WebBrowserObject>(webBrowserObject);
    }
    @Override
    public void commandReceived(WebBrowserEvent e, String command, String[] args) {
      WebBrowserObject webBrowserObject = this.webBrowserObject.get();
      if(webBrowserObject == null) {
        return;
      }
      if("WB_setLoaded".equals(command)) {
        Object[] listeners = webBrowserObject.listenerList.getListenerList();
        InitializationEvent ev = null;
        for(int i=listeners.length-2; i>=0; i-=2) {
          if(listeners[i] == InitializationListener.class) {
            if(ev == null) {
              ev = new InitializationEvent(webBrowserObject.source);
            }
            ((InitializationListener)listeners[i + 1]).objectInitialized(ev);
          }
        }
      }
    }
  }
  
  private Object source;
  private JWebBrowser webBrowser;
  private int instanceID;
  
  public WebBrowserObject(Object source, JWebBrowser webBrowser) {
    this.source = source;
    this.webBrowser = webBrowser;
    webBrowser.setBarsVisible(false);
    webBrowser.addWebBrowserListener(new NWebBrowserListener(this));
  }
  
  private String resourcePath;

  public String getLoadedResource() {
    return "".equals(resourcePath)? null: resourcePath;
  }
  
  public boolean hasContent() {
    return resourcePath != null;
  }
  
  private class CMLocal_waitForInitialization extends LocalMessage {
    @Override
    public Object run() {
      InitializationListener initializationListener = (InitializationListener)args[0];
      Boolean[] resultArray = (Boolean[])args[1];
      for(long time = System.currentTimeMillis(); !resultArray[0].booleanValue() && System.currentTimeMillis() - time < 4000; ) {
        NativeInterfaceHandler.syncExec(new EmptyMessage());
        try {
          Thread.sleep(50);
        } catch(Exception e) {}
      }
      removeInitializationListener(initializationListener);
      return null;
    }
  }
  
  @SuppressWarnings("deprecation")
  public void load(String resourcePath) {
    this.resourcePath = resourcePath;
    Registry.getInstance().remove(instanceID);
    if(resourcePath == null) {
      webBrowser.setText("");
      return;
    }
    Registry.getInstance().remove(instanceID);
    instanceID = Registry.getInstance().add(this);
    resourcePath = WebServer.getDefaultWebServer().getDynamicContentURL(WebBrowserObject.class.getName(), "html/" + instanceID);
    final Boolean[] resultArray = new Boolean[] {Boolean.FALSE};
    InitializationListener initializationListener = new InitializationListener() {
      public void objectInitialized(InitializationEvent e) {
        removeInitializationListener(this);
        resultArray[0] = Boolean.TRUE;
      }
    };
    addInitializationListener(initializationListener);
    webBrowser.setURL(resourcePath);
    webBrowser.getDisplayComponent().runSync(new CMLocal_waitForInitialization(), initializationListener, resultArray);
  }
  
  protected String getLocalFileURL(File localFile) {
    try {
      return localFile.toURI().toURL().toExternalForm();
    } catch(Exception e) {
    }
    return WebServer.getDefaultWebServer().getResourcePathURL(localFile.getParent(), localFile.getName());
  }

  protected static final String LS = System.getProperty("line.separator");

  protected static WebServerContent getWebServerContent(HTTPRequest httpRequest) {
    String resourcePath = httpRequest.getResourcePath();
    int index = resourcePath.indexOf('/');
    String type = resourcePath.substring(0, index);
    resourcePath = resourcePath.substring(index + 1);
    if("html".equals(type)) {
      final int instanceID = Integer.parseInt(resourcePath);
      final WebBrowserObject component = (WebBrowserObject)Registry.getInstance().get(instanceID);
      if(component == null) {
        return null;
      }
      return new WebServerContent() {
        @Override
        public InputStream getInputStream() {
          String javascriptDefinitions = component.getJavascriptDefinitions();
          javascriptDefinitions = javascriptDefinitions == null? "": javascriptDefinitions + LS;
          String content =
            "<html>" + LS +
            "  <head>" + LS +
            "    <script language=\"JavaScript\" type=\"text/javascript\">" + LS +
            "      <!--" + LS +
            "      function sendCommand(command) {" + LS +
            "        var s = 'command://' + encodeURIComponent(command);" + LS +
            "        for(var i=1; i<arguments.length; s+='&'+encodeURIComponent(arguments[i++]));" + LS +
            "        window.location = s;" + LS +
            "      }" + LS +
            "      function postCommand(command) {" + LS +
            "        var elements = new Array();" + LS +
            "        for(var i=1; i<arguments.length; i++) {" + LS +
            "          var element = document.createElement('input');" + LS +
            "          element.type='text';" + LS +
            "          element.name='j_arg' + (i-1);" + LS +
            "          element.value=arguments[i];" + LS +
            "          document.createElement('j_arg' + (i-1));" + LS +
            "          elements[i-1] = element;" + LS +
            "          document.j_form.appendChild(element);" + LS +
            "        }" + LS +
            "        document.j_form.j_command.value = command;" + LS +
            "        document.j_form.submit();" + LS +
            "        for(var i=0; i<elements.length; i++) {" + LS +
            "          document.j_form.removeChild(elements[i]);" + LS +
            "        }" + LS +
            "      }" + LS +
            "      function getEmbeddedObject() {" + LS +
            "        var movieName = \"myEmbeddedObject\";" + LS +
            "        if(window.document[movieName]) {" + LS +
            "          return window.document[movieName];" + LS +
            "        }" + LS +
            "        if(navigator.appName.indexOf(\"Microsoft Internet\") == -1) {" + LS +
            "          if(document.embeds && document.embeds[movieName]) {" + LS +
            "            return document.embeds[movieName];" + LS +
            "          }" + LS +
            "        } else {" + LS +
            "          return document.getElementById(movieName);" + LS +
            "        }" + LS +
            "      }" + LS +
            javascriptDefinitions +
            "      //-->" + LS +
            "    </script>" + LS +
            "    <style type=\"text/css\">" + LS +
            "      html, object, embed, div, body, table { width: 100%; height: 100%; min-height: 100%; margin: 0; padding: 0; overflow: hidden; background-color: #FFFFFF; text-align: center; }" + LS +
            "      object, embed, div { position: absolute; left:0; top:0;}" + LS +
            "      td { vertical-align: middle; }" + LS +
            "    </style>" + LS +
            "  </head>" + LS +
            "  <body height=\"*\">" + LS +
            "    <iframe style=\"display:none;\" name=\"j_iframe\"></iframe>" + LS +
            "    <form style=\"display:none;\" name=\"j_form\" action=\"" + WebServer.getDefaultWebServer().getDynamicContentURL(WebBrowserObject.class.getName(), "postCommand/" + instanceID) + "\" method=\"POST\" target=\"j_iframe\">" + LS +
            "      <input name=\"j_command\" type=\"text\"></input>" + LS +
            "    </form>" + LS +
            "    <script src=\"" + WebServer.getDefaultWebServer().getDynamicContentURL(WebBrowserObject.class.getName(), "js/" + instanceID) + "\"></script>" + LS +
            "  </body>" + LS +
            "</html>" + LS;
          return getInputStream(content);
        }
      };
    }
    if("js".equals(type)) {
      final int instanceID = Integer.parseInt(resourcePath);
      final WebBrowserObject webBrowserObject = (WebBrowserObject)Registry.getInstance().get(instanceID);
      if(webBrowserObject == null) {
        return null;
      }
      String url = webBrowserObject.resourcePath;
      // local files may have some security restrictions depending on the plugin, so let's ask the plugin for a valid URL.
      File file = Utils.getLocalFile(url);
      if(file != null) {
        url = webBrowserObject.getLocalFileURL(file);
      }
      final String escapedURL = Utils.escapeXML(url);
      return new WebServerContent() {
        @Override
        public String getContentType() {
          return getDefaultMimeType(".js");
        }
        public InputStream getInputStream() {
          ObjectHTMLConfiguration objectHtmlConfiguration = webBrowserObject.getObjectHtmlConfiguration();
          StringBuffer objectParameters = new StringBuffer();
          StringBuffer embedParameters = new StringBuffer();
          Map<String, String> parameters = objectHtmlConfiguration.getHTMLParameters();
          HashMap<String, String> htmlParameters = parameters == null? new HashMap<String, String>(): new HashMap<String, String>(parameters);
          htmlParameters.remove("width");
          htmlParameters.remove("height");
          htmlParameters.remove("type");
          htmlParameters.remove("name");
          htmlParameters.remove(objectHtmlConfiguration.getWindowsParamName());
          htmlParameters.remove(objectHtmlConfiguration.getParamName());
          for(Entry<String, String> param: htmlParameters.entrySet()) {
            String name = Utils.escapeXML(param.getKey());
            String value = Utils.escapeXML(param.getValue());
            embedParameters.append(' ').append(name).append("=\"").append(value).append("\"");
            objectParameters.append("window.document.write('  <param name=\"").append(name).append("\" value=\"").append(value).append("\"/>');" + LS);
          }
          String version = objectHtmlConfiguration.getVersion();
          String versionParameter = version != null? " version=\"" + version + "\"": "";
          String content =
            "<!--" + LS +
            "window.document.write('<object classid=\"clsid:" + objectHtmlConfiguration.getWindowsClassID() + "\" id=\"myEmbeddedObject\" codebase=\"" + objectHtmlConfiguration.getWindowsInstallationURL() + "\" events=\"true\">');" + LS +
            "window.document.write('  <param name=\"" + objectHtmlConfiguration.getWindowsParamName() + "\" value=\"' + decodeURIComponent('" + escapedURL + "') + '\";\"/>');" + LS +
            objectParameters +
            "window.document.write('  <embed" + embedParameters + " name=\"myEmbeddedObject\" " + objectHtmlConfiguration.getParamName() + "=\"" + escapedURL + "\" type=\"" + objectHtmlConfiguration.getMimeType() + "\" pluginspage=\"" + objectHtmlConfiguration.getInstallationURL() + "\"" + versionParameter+ ">');" + LS +
            "window.document.write('  </embed>');" + LS +
            "window.document.write('</object>');" + LS +
            "window.document.write('<div></div>');" + LS +
            "window.document.write('<div id=\"messageDiv\" style=\"display:none;\"><table><tr><td>" + objectHtmlConfiguration.getHTMLLoadingMessage() + "</td></tr></table></div>');" + LS +
            "setTimeout('document.getElementById(\\'messageDiv\\').style.display = \\'inline\\'', 200);" + LS +
            "var embeddedObject = getEmbeddedObject();" + LS +
            "embeddedObject.style.width = '100%';" + LS +
            "embeddedObject.style.height = '100%';" + LS +
            "sendCommand('WB_setLoaded');" + LS +
            "//-->" + LS;
          return getInputStream(content);
        }
      };
    }
    if("postCommand".equals(type)) {
      final int instanceID = Integer.parseInt(resourcePath);
      final WebBrowserObject webBrowserObject = (WebBrowserObject)Registry.getInstance().get(instanceID);
      if(webBrowserObject == null) {
        return null;
      }
      HTTPData postData = httpRequest.getHTTPPostDataArray()[0];
      Map<String, String> headerMap = postData.getHeaderMap();
      int size = headerMap.size();
      final String command = headerMap.get("j_command");
      final String[] arguments = new String[size - 1];
      for(int i=0; i<arguments.length; i++) {
        arguments[i] = headerMap.get("j_arg" + i);
        System.err.println(arguments[i]);
      }
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          WebBrowserListener[] webBrowserListeners = webBrowserObject.webBrowser.getWebBrowserListeners();
          WebBrowserEvent e = null;
          for(int i=webBrowserListeners.length-1; i>= 0; i--) {
            if(e == null) {
              e = new WebBrowserEvent(webBrowserObject.webBrowser);
            }
            webBrowserListeners[i].commandReceived(e, command, arguments);
          }
        }
      });
      return new WebServerContent() {
        @Override
        public InputStream getInputStream() {
          String content =
            "<html>" + LS +
            "  <body>" + LS +
            "    Command sent successfully." + LS +
            "  </body>" + LS +
            "</html>" + LS;
          return getInputStream(content);
        }
      };
    }
    return null;
  }
  
  protected static class ObjectHTMLConfiguration {
    
    private String htmlLoadingMessage;
    
    public void setHTMLLoadingMessage(String htmlLoadingMessage) {
      this.htmlLoadingMessage = htmlLoadingMessage;
    }
    
    public String getHTMLLoadingMessage() {
      return htmlLoadingMessage;
    }
    
    private String windowsClassID;
    
    public void setWindowsClassID(String windowsClassID) {
      this.windowsClassID = windowsClassID;
    }
    
    public String getWindowsClassID() {
      return windowsClassID;
    }
    
    private String windowsInstallationURL;
    
    public String getWindowsInstallationURL() {
      return windowsInstallationURL;
    }
    
    public void setWindowsInstallationURL(String windowsInstallationURL) {
      this.windowsInstallationURL = windowsInstallationURL;
    }
    
    private String installationURL;
    
    public String getInstallationURL() {
      return installationURL;
    }
    
    public void setInstallationURL(String installationURL) {
      this.installationURL = installationURL;
    }
    
    private String version;
    
    public String getVersion() {
      return version;
    }
    
    public void setVersion(String version) {
      this.version = version;
    }
    
    private String windowsParamName;
    
    public String getWindowsParamName() {
      return windowsParamName;
    }
    
    public void setWindowsParamName(String windowsParamName) {
      this.windowsParamName = windowsParamName;
    }
    
    private String paramName;
    
    public String getParamName() {
      return paramName;
    }
    
    public void setParamName(String paramName) {
      this.paramName = paramName;
    }
    
    private Map<String, String> htmlParameters;
    
    public Map<String, String> getHTMLParameters() {
      return htmlParameters;
    }
    
    public void setHTMLParameters(Map<String, String> htmlParameters) {
      this.htmlParameters = htmlParameters;
    }
    
    private String mimeType;
    
    public String getMimeType() {
      return mimeType;
    }
    
    public void setMimeType(String mimeType) {
      this.mimeType = mimeType;
    }
    
  }
  
  protected abstract ObjectHTMLConfiguration getObjectHtmlConfiguration();
 
  protected String getJavascriptDefinitions() {
    return null;
  }
  
  public void dispose() {
    webBrowser.dispose();
  }
  
  public boolean isDisposed() {
    return webBrowser.isDisposed();
  }

  private EventListenerList listenerList = new EventListenerList();
  
  public void addInitializationListener(InitializationListener listener) {
    listenerList.add(InitializationListener.class, listener);
  }
  
  public void removeInitializationListener(InitializationListener listener) {
    listenerList.remove(InitializationListener.class, listener);
  }
  
  public InitializationListener[] getInitializationListeners() {
    return listenerList.getListeners(InitializationListener.class);
  }

  /**
   * Set the value of a property of the object (a String, number, boolean).
   */
  public void setObjectProperty(String property, Object value) {
    webBrowser.execute("try {getEmbeddedObject()." + property + " = " + getObjectArgument(value) + ";} catch(exxxxx) {}");
  }
  
  /**
   * Call a function on the object and waits for a result, with optional arguments (Strings, numbers, booleans).
   */
  public String getObjectProperty(String property) {
    return webBrowser.executeAndWaitForCommandResult("[[getFlashResult]]", "try {sendCommand('[[getFlashResult]]', getEmbeddedObject()." + property + ");} catch(exxxxx) {sendCommand('[[getFlashResult]]', null);}");
  }
  
  /**
   * Call a function on the object, with optional arguments (Strings, numbers, booleans).
   */
  public void callObjectFunction(String functionName, Object... args) {
    webBrowser.execute("try {" + getObjectFunctionCall(functionName, args) + ";} catch(exxxxx) {}");
  }
  
  /**
   * Call a function on the object and waits for a result, with optional arguments (Strings, numbers, booleans).
   */
  public String callObjectFunctionWithResult(String functionName, Object... args) {
    return webBrowser.executeAndWaitForCommandResult("[[getFlashResult]]", "try {sendCommand('[[getFlashResult]]', " + getObjectFunctionCall(functionName, args) + ");} catch(exxxxx) {sendCommand('[[getFlashResult]]', null);}");
  }
  
  private String getObjectFunctionCall(String functionName, Object... args) {
    StringBuilder sb = new StringBuilder();
    sb.append("getEmbeddedObject().").append(functionName).append('(');
    for(int i=0; i<args.length; i++) {
      if(i > 0) {
        sb.append(", ");
      }
      sb.append(getObjectArgument(args[i]));
    }
    sb.append(")");
    return sb.toString();
  }
  
  private String getObjectArgument(Object arg) {
    if(arg == null) {
      return "null";
    }
    if(arg instanceof Boolean || arg instanceof Number) {
      return arg.toString();
    }
    arg = arg.toString();
    String encodedArg = Utils.encodeURL((String)arg);
    if(arg.equals(encodedArg)) {
      return '\'' + (String)arg + '\'';
    }
    return "decodeURIComponent('" + encodedArg + "')";
  }
  
}
