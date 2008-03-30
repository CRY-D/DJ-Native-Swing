/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 * 
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package chrriis.dj.nativeswing.components.win32;

/**
 * A Media Player object responsible for settings-related actions.
 * @author Christopher Deckers
 */
public class WMPSettings {
  
  private NativeWMediaPlayer nativeComponent;
  
  WMPSettings(JWMediaPlayer multiMediaPlayer) {
    this.nativeComponent = (NativeWMediaPlayer)multiMediaPlayer.getNativeComponent();
  }
  
  public void setErrorDialogsEnabled(boolean isErrorDialogEnabled) {
    nativeComponent.setOleProperty(new String[] {"settings", "enableErrorDialogs"}, isErrorDialogEnabled);
  }
  
  public void setVolume(int volume) {
    if(volume < 0 || volume > 100) {
      throw new IllegalArgumentException("The volume must be between 0 and 100");
    }
    nativeComponent.setOleProperty(new String[] {"settings", "volume"}, volume);
  }
  
  /**
   * @return The volume, between 0 and 100. When mute, the volume is still returned. -1 indicate that it could not be accessed.
   */
  public int getVolume() {
    try {
      return (Integer)nativeComponent.getOleProperty(new String[] {"settings", "volume"});
    } catch(Exception e) {
      return -1;
    }
  }
  
  /**
   * @param stereoBalance The stereo balance between -100 and 100, with 0 being the default.
   */
  public void setStereoBalance(int stereoBalance) {
    if(stereoBalance < 100 || stereoBalance > 100) {
      throw new IllegalArgumentException("The stereo balance must be between -100 and 100");
    }
    nativeComponent.setOleProperty(new String[] {"settings", "balance"}, stereoBalance);
  }
  
  /**
   * @return The stereo balance, between -100 and 100, with 0 being the default. When mute, the balance is still returned.
   */
  public int getStereoBalance() {
    try {
      return (Integer)nativeComponent.getOleProperty(new String[] {"settings", "balance"});
    } catch(Exception e) {
      return -1;
    }
  }
  
  public void setAutoStart(boolean isAutoStart) {
    nativeComponent.setOleProperty(new String[] {"settings", "autoStart"}, isAutoStart);
  }
  
  public boolean isAutoStart() {
    return Boolean.TRUE.equals(nativeComponent.getOleProperty(new String[] {"settings", "autoStart"}));
  }
  
  public void setMute(boolean isMute) {
    nativeComponent.setOleProperty(new String[] {"settings", "mute"}, isMute);
  }
  
  public boolean isMute() {
    return Boolean.TRUE.equals(nativeComponent.getOleProperty(new String[] {"settings", "mute"}));
  }
  
  public boolean isPlayEnabled() {
    return Boolean.TRUE.equals(nativeComponent.getOleProperty(new String[] {"controls", "isAvailable"}, "Play"));
  }
  
}