package com.ventrys.job.client.renderer.item;

import com.ventrys.job.item.MoutonSansLaineCarcasseItem;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class MoutonSansLaineCarcasseRenderer extends GeoItemRenderer<MoutonSansLaineCarcasseItem> {
   public MoutonSansLaineCarcasseRenderer() {
      super(new MoutonSansLaineCarcasseModel());
   }
}
