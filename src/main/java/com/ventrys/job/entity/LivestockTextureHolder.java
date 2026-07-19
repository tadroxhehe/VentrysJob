package com.ventrys.job.entity;

import java.util.List;

/** Animal d'élevage avec variante de texture fixable (œuf de spawn dédié). */
public interface LivestockTextureHolder {

    void setTextureVariant(String texture);

    String getTextureVariant();

    static List<String> textureVariantsFor(LivestockTextureHolder holder) {
        if (holder instanceof CustomCow) {
            return CustomCow.getTextureVariantIds();
        }
        if (holder instanceof CustomPig) {
            return CustomPig.getTextureVariantIds();
        }
        if (holder instanceof CustomChicken) {
            return CustomChicken.getTextureVariantIds();
        }
        if (holder instanceof CustomSheep) {
            return CustomSheep.getTextureVariantIds();
        }
        return List.of();
    }
}
