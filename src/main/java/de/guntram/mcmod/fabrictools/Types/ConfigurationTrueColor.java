/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.fabrictools.Types;

/**
 *
 * @author gbl
 */
public class ConfigurationTrueColor {
    public int red, green, blue;
    
    public ConfigurationTrueColor(int rgb) {
        red = (rgb >> 16 ) & 0xff;
        green = (rgb >> 8) & 0xff;
        blue = rgb & 0xff;
    }
    
    public int getInt() {
        return red << 16 | green << 8 | blue;
    }
}
