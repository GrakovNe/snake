package org.grakovne.snake

class BodyItem(val first: Int, val second: Int) {

        override fun equals(other: Any?): Boolean {
            return this.first == (other as BodyItem).first && this.second == other.second
        }

        override fun hashCode(): Int {
            return 31 * first + second
        }

    }