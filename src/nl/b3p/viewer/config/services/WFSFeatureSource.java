/*
 * Copyright (C) 2011 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.viewer.config.services;

import java.io.IOException;
import java.util.List;
import javax.persistence.*;

/**
 *
 * @author Matthijs Laan
 */
@Entity
@DiscriminatorValue("wfs")
public class WFSFeatureSource extends FeatureSource {

    @Override
    List<String> calculateUniqueValues(SimpleFeatureType sft, String attributeName, int maxFeatures) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
