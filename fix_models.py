#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import os
import re

def fix_model(input_file, output_file, texture_key=None, texture_value=None):
    """Fix a Blockbench model: add cullface to down faces and fix texture references."""
    with open(input_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    # Fix textures
    if texture_key and texture_value:
        if 'textures' not in data:
            data['textures'] = {}
        data['textures'][texture_key] = texture_value
        data['textures']['particle'] = texture_value
    
    # Add cullface: "down" to all "down" faces
    if 'elements' in data:
        for element in data['elements']:
            if 'faces' in element and 'down' in element['faces']:
                if 'cullface' not in element['faces']['down']:
                    element['faces']['down']['cullface'] = 'down'
    
    # Write output with proper formatting (tabs)
    with open(output_file, 'w', encoding='utf-8') as f:
        json_str = json.dumps(data, indent='\t', ensure_ascii=False)
        f.write(json_str)
    
    print(f"Fixed: {output_file}")

# Fix artisan_table.json (use table_menuisier.json with table_menuisier3 texture)
fix_model(
    'json_textures/table artisanat/table_menuisier.json',
    'src/main/resources/assets/ventrysjob/models/block/artisan_table.json',
    texture_key='2',
    texture_value='ventrysjob:block/table_menuisier3'
)

# Fix metier_tisser.json (use couturier.json with couturier texture)
fix_model(
    'json_textures/metier_à_tisser/couturier.json',
    'src/main/resources/assets/ventrysjob/models/block/metier_tisser.json',
    texture_key='0',
    texture_value='ventrysjob:block/couturier'
)

# Fix existing models: add cullface to down faces
for model_file in [
    'src/main/resources/assets/ventrysjob/models/block/apothicaire_table.json',
    'src/main/resources/assets/ventrysjob/models/block/forgeron_table.json',
    'src/main/resources/assets/ventrysjob/models/block/cuisinier_table.json'
]:
    if os.path.exists(model_file):
        fix_model(model_file, model_file)

print("All models fixed!")

