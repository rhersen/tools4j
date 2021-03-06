/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.tools4j.config.examples.family;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.deephacks.tools4j.config.Config;
import org.deephacks.tools4j.config.Id;

/**
 * A recursive example of a family tree. 
 * 
 * See the parent-child.png for an illustration of the potential realtionship 
 * between marriage and persons.
 */
@Config(desc = "Users", name = "Person")
public class Person extends AbstractPerson {
    @Id(desc = "id")
    @NotNull
    protected String id;

    @Config(desc = "This person is the parent of childs.")
    public List<Person> children = new ArrayList<Person>();

    public List<Person> getChildren() {
        return children;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Person other = (Person) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}
