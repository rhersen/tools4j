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
package org.deephacks.tools4j.config.admin.rcp.model;

import org.deephacks.tools4j.config.model.Bean;
import org.deephacks.tools4j.config.model.Bean.BeanId;
import org.deephacks.tools4j.config.model.Schema;

public class Events {
    public RcpSchema schema;

    public Events(RcpSchema actionTarget) {
        this.schema = actionTarget;
    }

    public static class PreCreateBeanEvent {
        private Schema schema;

        public PreCreateBeanEvent(Schema schema) {
            this.schema = schema;
        }

        public Schema getSchema() {
            return schema;
        }
    }

    public static class ViewBeanEvent {
        private Bean bean;

        public ViewBeanEvent(Bean bean) {
            this.bean = bean;
        }

        public Bean getBean() {
            return bean;
        }
    }

    public static class PostCreateBeanEvent {
        private Bean bean;

        public PostCreateBeanEvent(Bean bean) {
            this.bean = bean;
        }

        public Bean getBean() {
            return bean;
        }
    }

    public static class BeansChangedEvent {

    }

    public static class DeleteBeanEvent {
        private BeanId id;

        public DeleteBeanEvent(BeanId id) {
            this.id = id;
        }

        public BeanId getId() {
            return id;
        }
    }

    public static class PreMergeBeanEvent {
        private Bean bean;

        public PreMergeBeanEvent(Bean bean) {
            this.bean = bean;
        }

        public Bean getBean() {
            return bean;
        }
    }

    public static class PostMergeBeanEvent {
        private Bean bean;

        public PostMergeBeanEvent(Bean bean) {
            this.bean = bean;
        }

        public Bean getBean() {
            return bean;
        }
    }

    public static class ErrorEvent {
        private Throwable exception;

        public ErrorEvent(Throwable exception) {
            this.exception = exception;
        }

        public Throwable getThrowable() {
            return exception;
        }
    }

}
