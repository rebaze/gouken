/*
 * Copyright 2009 Toni Menzel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okidokiteam.gouken.macosx.activity;

import java.util.Map;
import com.okidokiteam.gouken.macosx.Command;

/**
 * @author Toni Menzel
 * @since Mar 4, 2010
 */
public class UnknownCommand implements Command
{

    public UnknownCommand( Map<String, String> map )
    {
    }

    public void execute()
    {
        throw new RuntimeException( "Try start | stop | restart | status" );
    }
}
