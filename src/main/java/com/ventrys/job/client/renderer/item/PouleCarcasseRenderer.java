package com.ventrys.job.client.renderer.item;

import com.ventrys.job.item.PouleCarcasseItem;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class PouleCarcasseRenderer extends GeoItemRenderer<PouleCarcasseItem> {
   public PouleCarcasseRenderer() {
      super(new PouleCarcasseModel());
   }
}
