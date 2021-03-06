/*
 * Copyright 2011-2012 Conventions Framework.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.conventionsframework.producer;

import org.conventionsframework.qualifier.Log;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

/**
 *
 * @author VAIO
 */
@Dependent
public class LoggerProducer implements Serializable{

    public LoggerProducer() {
    }
    
    @Produces @Log
    public  Logger produce(InjectionPoint ip){
        return  Logger.getLogger(ip.getMember().getDeclaringClass().getSimpleName());
    }
    
}
