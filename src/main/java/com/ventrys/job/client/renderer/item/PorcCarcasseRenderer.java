package com.ventrys.job.client.renderer.item;

import com.ventrys.job.item.PorcCarcasseItem;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class PorcCarcasseRenderer extends GeoItemRenderer<PorcCarcasseItem> {
   public PorcCarcasseRenderer() {
      super(new PorcCarcasseModel());
   }
}
