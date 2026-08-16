package com.ventrys.job.client.renderer.item;

import com.ventrys.job.item.VacheCarcasseItem;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class VacheCarcasseRenderer extends GeoItemRenderer<VacheCarcasseItem> {
   public VacheCarcasseRenderer() {
      super(new VacheCarcasseModel());
   }
}
