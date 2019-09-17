package com.TheRPGAdventurer.ROTD.server;

import com.TheRPGAdventurer.ROTD.common.CommonProxy;
import net.minecraftforge.fml.server.FMLServerHandler;

import java.io.File;

/**
 * Created by TGG on 3/08/2019.
 */
public class ServerProxy extends CommonProxy {

  @Override
  public File getDataDirectory() {
    return FMLServerHandler.instance().getSavesDirectory();
  }

  @Override
  public boolean isDedicatedServer() {return true;}

}
