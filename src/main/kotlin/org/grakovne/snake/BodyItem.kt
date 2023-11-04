package org.grakovne.snake

class BodyItem(val first: Int, val second: Int) {

        override fun equals(other: Any?): Boolean {
            return this.first == (other as BodyItem).first && this.second == other.second
        }

        override fun hashCode(): Int {
            var result = first
            result = 31 * result + second
            return result
        }

    }