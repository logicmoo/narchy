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

package org.boon.validation.validators;


import org.boon.Str;
import org.boon.validation.ValidatorMessage;
import org.boon.validation.ValidatorMessageHolder;

/**
 * <p>
 * <small>
 * Required validator.
 * </small>
 * </p>
 *
 * @author Rick Hightower
 */
public class RequiredValidator extends BaseValidator {

    public void init() {
        if ( Str.isEmpty( this.getDetailMessage() ) ) {
            this.setDetailMessage( "{validator.required.detail}" );
        }

        if ( !this.isNoSummary() ) {
            if ( Str.isEmpty( this.getSummaryMessage() ) ) {
                this.setSummaryMessage( "{validator.required.summary}" );
            }
        }
    }

    public ValidatorMessageHolder validate( Object object, String fieldLabel ) {
        ValidatorMessage message = new ValidatorMessage();

        if ( object instanceof String ) {
            String string = ( String ) object;
            boolean valid = string != null && !string.trim().isEmpty();
            if ( !valid ) {
                populateMessage( message, fieldLabel );
            }

        } else {
            if ( object == null ) {
                populateMessage( message, fieldLabel );
            }
        }

        return message;
    }


}