package com.ventrys.job.client.renderer.item;

import com.ventrys.job.item.MoutonCarcasseItem;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class MoutonCarcasseRenderer extends GeoItemRenderer<MoutonCarcasseItem> {
   public MoutonCarcasseRenderer() {
      super(new MoutonCarcasseModel());
   }
}
