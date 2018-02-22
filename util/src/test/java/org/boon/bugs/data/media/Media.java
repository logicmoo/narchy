/*
 * Copyright 2013-2014 Richard M. Hightower
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * __________                              _____          __   .__
 * \______   \ ____   ____   ____   /\    /     \ _____  |  | _|__| ____    ____
 *  |    |  _//  _ \ /  _ \ /    \  \/   /  \ /  \\__  \ |  |/ /  |/    \  / ___\
 *  |    |   (  <_> |  <_> )   |  \ /\  /    Y    \/ __ \|    <|  |   |  \/ /_/  >
 *  |______  /\____/ \____/|___|  / \/  \____|__  (____  /__|_ \__|___|  /\___  /
 *         \/                   \/              \/     \/     \/       \//_____/
 *      ____.                     ___________   _____    ______________.___.
 *     |    |____ ___  _______    \_   _____/  /  _  \  /   _____/\__  |   |
 *     |    \__  \\  \/ /\__  \    |    __)_  /  /_\  \ \_____  \  /   |   |
 * /\__|    |/ __ \\   /  / __ \_  |        \/    |    \/        \ \____   |
 * \________(____  /\_/  (____  / /_______  /\____|__  /_______  / / ______|
 *               \/           \/          \/         \/        \/  \/
 */
package org.boon.bugs.data.media;

import java.util.List;

@SuppressWarnings("serial")
public class Media implements java.io.Serializable {

    public enum Player {

        JAVA, FLASH;

        public static Player find(String str) {
            if (str == "JAVA")
                return JAVA;
            if (str == "FLASH")
                return FLASH;
            if ("JAVA".equals(str))
                return JAVA;
            if ("FLASH".equals(str))
                return FLASH;
            String desc = (str == null) ? "NULL" : String.format("'%s'", str);
            throw new IllegalArgumentException("No Player value of " + desc);
        }
    }

    public String uri;

    // Can be unset.
    public String title;

    public int width;

    public int height;

    public String format;

    public long duration;

    public long size;

    // Can be unset.
    public int bitrate;

    public boolean hasBitrate;

    public List<String> persons;

    public Player player;

    // Can be unset.
    public String copyright;

    public Media() {
    }

    public Media(String uri, String title, int width, int height, String format, long duration, long size, int bitrate, boolean hasBitrate, List<String> persons, Player player, String copyright) {
        this.uri = uri;
        this.title = title;
        this.width = width;
        this.height = height;
        this.format = format;
        this.duration = duration;
        this.size = size;
        this.bitrate = bitrate;
        this.hasBitrate = hasBitrate;
        this.persons = persons;
        this.player = player;
        this.copyright = copyright;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Media media = (Media) o;
        if (bitrate != media.bitrate)
            return false;
        if (duration != media.duration)
            return false;
        if (hasBitrate != media.hasBitrate)
            return false;
        if (height != media.height)
            return false;
        if (size != media.size)
            return false;
        if (width != media.width)
            return false;
        if (copyright != null ? !copyright.equals(media.copyright) : media.copyright != null)
            return false;
        if (format != null ? !format.equals(media.format) : media.format != null)
            return false;
        if (persons != null ? !persons.equals(media.persons) : media.persons != null)
            return false;
        if (player != media.player)
            return false;
        if (title != null ? !title.equals(media.title) : media.title != null)
            return false;
        if (uri != null ? !uri.equals(media.uri) : media.uri != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + bitrate;
        result = 31 * result + (hasBitrate ? 1 : 0);
        result = 31 * result + (persons != null ? persons.hashCode() : 0);
        result = 31 * result + (player != null ? player.hashCode() : 0);
        result = 31 * result + (copyright != null ? copyright.hashCode() : 0);
        return result;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
        this.hasBitrate = true;
    }

    public void setPersons(List<String> persons) {
        this.persons = persons;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getFormat() {
        return format;
    }

    public long getDuration() {
        return duration;
    }

    public long getSize() {
        return size;
    }

    public int getBitrate() {
        return bitrate;
    }

    public List<String> getPersons() {
        return persons;
    }

    public Player getPlayer() {
        return player;
    }

    public String getCopyright() {
        return copyright;
    }
}