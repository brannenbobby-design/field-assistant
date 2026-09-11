package com.brannenservices.fieldassistant

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

/** Real animation frames cropped from the approved Florida Man sprite sheet. */
object AnimatedRaster {
    private fun decode(w: Int, h: Int, pal: IntArray, data: String): Bitmap {
        val compressed = Base64.decode(data, Base64.DEFAULT)
        val raw = GZIPInputStream(ByteArrayInputStream(compressed)).use { it.readBytes() }
        val pixels = IntArray(w * h)
        for (i in pixels.indices) {
            val value = raw[i].toInt() and 255
            pixels[i] = if (value == 0) 0 else pal[value - 1]
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private val manPal = intArrayOf(0xFFF3DFD0.toInt(), 0xFFE3B9A6.toInt(), 0xFFF3995E.toInt(), 0xFFB89C92.toInt(), 0xFFE27E4B.toInt(), 0xFFBE7A55.toInt(), 0xFF60779E.toInt(), 0xFFC95F37.toInt(), 0xFF906958.toInt(), 0xFFA24D2E.toInt(), 0xFF2E5086.toInt(), 0xFF634947.toInt(), 0xFF7F3C27.toInt(), 0xFF2C3350.toInt(), 0xFF592A20.toInt(), 0xFF2E1C21.toInt())
    private val flamingoPal = intArrayOf(0xFFF6DAD0.toInt(), 0xFFF4A7AF.toInt(), 0xFFFA899D.toInt(), 0xFFF86E8B.toInt(), 0xFFF0587B.toInt(), 0xFFC46778.toInt(), 0xFFE3466D.toInt(), 0xFFCE375F.toInt(), 0xFF955963.toInt(), 0xFFB52E54.toInt(), 0xFF9B294A.toInt(), 0xFF643B48.toInt(), 0xFF81203F.toInt(), 0xFF641A33.toInt(), 0xFF461729.toInt(), 0xFF200E18.toInt())

    val manWalk1 by lazy { decode(44, 64, manPal,
        "H4sIAMIbpGoC/+1WC3IsIQjcUXFEQbz/aV+ju8l+nCQHeKRqaz892DTQ5nb7Hz9G751FWP+AbO0MByKw/JaznS04MJ0H/4xt59l6"
        + "AlbCeQT7hUBr/TyOJMd5/gHb5Jh8U9DbGL9gG4CxFCKqWlV+wLpkmShSUTNTvSAi0rPi11r8VXWYjQso54pDK6B4aJRq12yBLPgb"
        + "HmfDS+/jKmlBKE63Eq2PYTqu8mrxtEakRrGKaYwgsYVWpAVb0+xYBJSIZV8YcnpZqNu0umDz455CJVVAHWuGD3gEb/fKZiSCRsP8"
        + "AQSz1cvSpvjKKQKCpASZL0q7WY7kPEOIM7M/u8cqK+fIjPLjEYuT9mHYYbVG4oyAVjkcNGmMbW1agcqZ53gBGyhPGmMnVwRuMiho"
        + "AMYc8paSN/oqoDkLkjqW4tyJkFOo9SNxme10JGEiCIvJiZmTfI450kIC7MvsavG0ImqJZdR3O8E8MftmoRGUQ5hYj2HlDWscCedj"
        + "AAQUQ4qQIaQUnEN5J5HJKaBkdAO4ue7JCMpAn1clzFWIftrQtJDAVspiLuALizrnuvpTwLqTuWJoNUMTH+NSX6Br/o1XZQdLSpkN"
        + "ZuJorNJjySYW+7WgdxnYGBuKoFjAehEZeUIJiaHDvTRhMUy6OjhD/rhcysqiS+NmvQu65blTAAUff/cAMLkXWCv5ORVQOK+7GbYH"
        + "IbwoFu/nl2Y+u5i+0WHSZ2oJDll1rGNHxcA9a4zm+nKO0VJrOeDXeATVx2irbi4KzyxAoj6ikO/fb+Zd2gmvExcNmemgeun/1pOb"
        + "tKBUeFV0eK6X2Aa2MEfwK0euLiVdXIawg37C/N13Uq7T/OIea7BSuEw7+8DEzwFD2NZ5feH8Kund22W+StFX5bbZ42m3BS3pt+fW"
        + "28Zz5vTEJ5kusHZP66b+/eVk/CbEZBYXWfvgVV9y5jvV9zJW4ipPDdBI62rQT20mCXucZiLrqLG9PyY1weUlKzHPhd9h18UlXTIt"
        + "2oK2wrzG5tofjs3NfVZWrm5+O/JmAGde9L7q41bGFE7H+Owm1gerNPrXnYym9uWJHyzwywyR7/8txorPvCvuyH+wtamrAAsAAA=="
    ) }

    val manWalk2 by lazy { decode(44, 64, manPal,
        "H4sIAMIbpGoC/+WW25LiMAxEiW0plrAs/f/XbsuhanZ2HJj3dQFFyInQpd3J4/E/rjl9YP0GnM/zwCoyP6FPrHIcrQH+BTqTPZsU"
        + "/4g+ny3ZMs+3bFws8k28/ZI9Sqfa1cz8LTufhXon4m5uesOqh4ljIZwpjiIPtqiQ4l/BKA8wXeN2HqLWlbt7OHKZEXPGFnTtWElr"
        + "GOsAahF79mEg1byTeq91AC19zzIR9WyQsRnYa/Utq5S9XKWHKdVKSId0y1LWj7eFZQdw6LftYpy9Tit6atnoPN5PwXII5kqUl5hz"
        + "D1ywQweKapSLS/VYk8PHTcukdc7V+aAF+y3rDZVXYlTPpVu8YQ1hiatIauuoPUeG6PuhMdg+hKWyHgejffG46ULnrIyZRBD3KI35"
        + "pq6HkRRIDLPrXJXZmsiNxHNOpXbH3BhagCxbNtu2YStZQaswiOzGage+s+7iJlsKG7pW0WOpFfJBsbZLl4wTDgiMhXnQpUnfswNe"
        + "czB0WE0y6h376GRaABfOrQTVL7Vvc0Bgk4wLr5G12zN+rXsbsawtzaYUidzIlttv3+KIK24pzX24jLUvbqYBg+YLliHG46PtQjtI"
        + "ojUe8c4fz+N8nrB/aFHQDBvvvBTocc7znAOzK0Xjne2emcXES2uBFsRuU2jJFrwnNmfv5ShuN104kcKZ+FyDOwqMZd+yAffMJM4z"
        + "cgqtcA5Z7zzSrtAT13FqM5fuvRdCsYHCUH2k+Sx2q5wlFNAvuw1VpACdbqKmgyb7VU1GJvpZnS+Ovqk1dC91qy9d/zXXG9avOvp3"
        + "BRht2GX3KOsfM7haod9t7PX/P6vIHddf5prHV1rUNzsgrjKg5LSsVMxVAvvWt/KUzBwTfHik/irj+x1LE2dlTE99jYHX1pRXfzgE"
        + "5HUH92cIwuL+voubEplf9+QxcMNEwT/TgKeCnF+/DygXj22CNMZG0+tRY/h1asBE8Fv+urGXWO1dD4B/AL3X5jwACwAA"
    ) }

    val manWalk3 by lazy { decode(44, 64, manPal,
        "H4sIAMIbpGoC/+1WC3bbMAyL9aEpmhR1/9MOlLNuXST3AmNe0r4YhvkBobxe/+MpendFjB+Bo/frPCKa/sh5TmgpqRR/Zr3O87om"
        + "tpyHP0MjegrscRV7Tjagvcx8S1H18Yy9IuGUmSuzmfkzVokjxN1NdsyizmIebHjbGD520MYCqLm49mHS+zZbsnguebCdfYyxxyqJ"
        + "iOHtkg1Qp7FLoEllYL2Kc674r2bUtoaSoEHuAx+cERWfssYqqppdwsMNxIz7tq0V8CIFQN3lRvpmxvPyCFKfQSS05bVogRilapNa"
        + "hm3F4KWi+sr1qD5u7h12AMqZWmNKPPMYO6y3kqVmUm18FJ5K2HT3ZYUyUxREUo6M5gG9K6xQrShPqXI5UhXarbEzF2KpIG7N2qHY"
        + "tt0SQwRIAaNotTaiok3bhtYkcztCNEaWQwZYNN+lkKUdiT06gVdGmepiG6y1lDAxriExwg2CP7ZCG2RVYArZAaaG11TlUpOCXCmF"
        + "hYXC0Y+pYDAvClRs5PSP1KAfrrXe2OW+YyWRb4AxgxDchFbbqCwdAU6pDR2hdkA3KzS6lsmbzNVLc4xy3TS4LhyvtzLRJaAv223Q"
        + "6AUWffWhDRHL/OSlwJZ0gX40MYxlb9Owx3MSX2cXwriZ2t75r7P0sOmuKZYJnd4w9/O4LRpgh0kjh20bPJBp0qIsOWi2d40FQJEG"
        + "koVwNZHMGa/r84ptjDOwhzh0mh++8uVmBg8Lhjfv9NAxXEUWrJRDWH+P/7ZV+iCGjefJ698UHV/551IEEC3Sb9gv/UJMf7BRSOXx"
        + "z8PqOwnDmvzOznjS2kdmd71dYQhvD/K5Aov23ODcsbftPrvuVi47OS+1TlXfxxxtWGOY8UTq7n1eHn7v63ry0cs4Z2/WSDzD52xp"
        + "dIae6dcxC10ZnLwNX63s8G8HsuNGqov+vjfrY9nBHD9WfvqF9VKc/IpUcJI8Y38B+VFTNwALAAA="
    ) }

    val manWalk4 by lazy { decode(44, 64, manPal,
        "H4sIAMIbpGoC/61WwXbkIAybYCCG2Nj//7Urk3a3zZC0h+X15TBVhCxkk9frPyx3M/z9AjnG2Le0bVl+5BzHsW9AppTbD9Bj3wOb"
        + "jpRz0mcBxzF500h5T/ZM+4nNW9qT/IwdqGwjKoW7mf6EJS7M3HtX7XaPFXF35RqcYfSdz9Kkc1djbK0gfTgRadhX1U39GK7dh/ud"
        + "WmHV2oPMDwixe2zVqAVPK2SheT5vSEOAQ68x4Q0tVO/U9qhKzaI0onCX4O+St5fSo/TQyYCFtSBf0moP63tINONp2q1jgHL826cP"
        + "ZpUMVd5g5zHF/jqxXAK7tgFKlUspdaM+O0jjjRvLtKOeShN85uA2C1or1VLhVdoKz+70G1prrVGtAixSXji8uztgySKt1UbEeQsw"
        + "W603CrB7rS0klGiKRLmmdQvBoQqxUomZ0tYyVsI+a1pSqYVgV4eE5gh9bqpLWoo0IATG2HprItCPGK2wJQxArKyXhDUl5NaWvFRg"
        + "AFjxrAmzLLXcAqtLDUBBLIwALXxwKG6dIk1XrEa4iaWCdQM4AwlsQei5v/uAY0AcKm3bx5CkzjPz5RJ4RYdV6igQWtNUmxuw8TpV"
        + "v3ZQ+NC1BW/25jhyRaMgfOWK1dmOQIfgFthoUp4/d7l224ktGsVl1IboKoYAzsjeXZu0jGziHABvHiVjXL6FDa14gocPiSlhc+Po"
        + "5vfT6NgfS/eY6SiqPVwVBocY3XA09D4D3x/AMRp8DGDD4+iLh6vCxoFbMGIGYIHL+gCVHViNWcVgrvxwyVpg0ee90xbay01rwhyH"
        + "Qt9lDMsZMzAc7OvC0MfoIxZUN8QwrKbbvm7N84RZx8CsntiywKqdWcDg48/Dn9Mak+rbtMHcPDkhIIL9UY73M0hfb0mRMwcBjbR+"
        + "Vu7nVt+aDerO/eNC/TIV/dztGxaXHZ8/2rXacsWCGNOIyntGoy8v2JdAs7xjX6dtevkUqqSroTw9n5nwfymABFZbzaJYzb9cchhG"
        + "dSH4xJYm3uJC+KtiyOI7ZY6tBoUYEOL/vggRroVetPuQAmoZ/vxNh0IQ0BFE/gx9zW+Sj/Wbj9Cv6w9FWOOVAAsAAA=="
    ) }

    val manJump by lazy { decode(52, 64, manPal,
        "H4sIAMIbpGoC/7WX65brIAiFq4gRi+L7P+3ZmHQurclpf4zNyprV5guwQXBut79YfQzt3T4h+v2eQghR9F3ivm0TSTFkfR9xJlra"
        + "gryDjPtcGxiNW2z2GZNCSvKeaw8mRKzcavkfYlk6wun3RMy4Sqm1tgtAVfwRac0a3o9VbRj+vkAygFJLw8pVzWBhgL+SQct8Nddm"
        + "Y/R7H2q9j8tQMp5vtTXcGxdF+QAd10zb4zUqVogUCBXcrhBxoFZDzFYJkjER5euUKjvioQxnCDJDarh24Z8WKGRAILS5my44TPq3"
        + "eNkyGm3TM5E6n4XSGfdyUQUmRCUjgBDJY7KWAZjxKVEzSaYMjlNgrsMh93GXAJG9KJZhgUUkszORuDnkq/AstpdwNDMrDKnQtBPY"
        + "ISQUH2hRiivx21DOAHL2dPDOwBBDDpXmdeTC3Z7KFHHnmY49IxEbJ8yUxpiay7ESABDN4qxwgbx14Onkl5yWtPuEnNiA625GsI1U"
        + "k1hKp5XjflW7ISNMEYz6sgRSzs2g+HXql9HR3K8kkmBrZcYmQXDsKIU4BQggHIsrM9XbBCFt+28KZkJxiCFbcdUGjKeVVr7tTChp"
        + "lLQ2g1p2hkd/1MPumpOQOsqy3fgepoJucbhGDk2/FPcT0bBRFE2z7wMEqoXo6YHKWKhTtXHWncfsub3LtONWsPNSqYmrN6GXov4y"
        + "4yOkJ7flOSo5lhjdd0RcX8fgMdrC5ianY2h1oVDYEUDtahokmOtDUHdUAj8YWslhfUsYH4B8lCp8ipUi2vycDWsGo8Pn4YaYnMHG"
        + "yBU5rbxvrNUwac39mhcUgWs+F7jVPRhs7lWjRmK34CY6BoEvb452CLBCqr+vdjeTfoyOxofQr0g9xGHzrI5v2zMWGFkgNNuNuzC+"
        + "kLET+/5dI96i5nB7FJSV6AiIRWufQ4Nmz/nhwuh1Dp+yrOyHmk8uaJ9BLhnLB1KfywIjWyDLC4CGdnj9Io236i4vJaPaym5kVbRd"
        + "icTG758w+8ueseUJDQ2e0PufmN52x07Gnnaf3c/S8ElhHO8cfpKx30ifA+R8/PshxJ5ilDufZe3kLABdfBS2D5iuFbKIjg8O0AMM"
        + "Q89PmK6K8YlW/cnZfnhF8SfhzB7oO/7j/yL6uP3x+gfHTlo1AA0AAA=="
    ) }

    val flWalk1 by lazy { decode(48, 64, flamingoPal,
        "H4sIAMIbpGoC/6VWi7KrOgitBgKGQPz/r70Ld0/P1rbqnJtpZ9QsYPFMHo//sRqW3QVbYy6liLZbaG08YxVivWNChctc2MNZ7nAC"
        + "G6oeyzqY7hiIhdtYlymUSWy5QI86T7Ot6zx1IRL1c3iv0zxNNWKujfgSPwDv0zRXt9TOl/gO7RPNixKiKSTtFL/2ZUxTaXWoIMFc"
        + "rvDWEZhqY0SECfifJ2AdY+119Hx2UKIL+olfxzMNtVzRfwC9rv1ZFlv9XIT/pd6UCull+fQn3ivgcqN64ke9lJn4dsNYTbhaf9Zr"
        + "03ZqihOOhGVfqsATuN5EvnSbS+5L0/SaqUwz2gcy34qpYVvEAEcbo/am7E388Nf2bsNkKrz5ivoBFFmG9iSYbSr6QX36itaSMhXK"
        + "hW5OSiwQ4YOFluypOTfFSGH0TBKZZyZY2gzvI+W8RcPUGHKJKimS7jIe0ER7gWAQ4BapnjbgjD5LVoiPgFjZT6XEZ+Qc/cg8/zjA"
        + "6DTJgkIH6X6MheS+gg5nRNJ52sL/FCBV/u2za/qYtrEw6gDijReAwg1pFttVR3YtBDb1aGFgRROaBQJfIaA7jw2albN3EW7d3vBH"
        + "zWkLB17hwA7fnuqk5fyxhKSg40DAVkrTLmeBgW65gQHBjGf/WfjWth0MPt8LtCz51AtKiNUGTPWYMkgM9MQ+x+YQAVlv6Ufi8Wwx"
        + "oBaGMJnirS+x3EfY5qzna/xy0T/3co452GgYeLui+TZlYNmGw+Phtxo/QDtAxixu4QeYpxhickvAxLd5BIE7+CHi8acKbVziXV5q"
        + "nfnysA+ll9KoLBcWwtA38ZJF1i7wCwb6K+7D+CpGSFb0v+JVzpMGFz127+eEjHZwTA45VU/HHDU6OwQaHc0bte+EgmS8VVP5HiIt"
        + "byUznL+e31Hqu6qh5P3bEdbiQ8aRs4+Mgj8V8Ii8tRw21sdjcaJ3z0Z4Ds/DxrLgnpdT6oj3ivulcuyjFrVaDgQ+VmPHdUujHSZV"
        + "7+ugGiP1jP4rFtF7p7ESbqW/P/eOC9NYMXegfRe6yLvR6ED3f7zC/wd4llsaAAwAAA=="
    ) }

    val flWalk2 by lazy { decode(48, 64, flamingoPal,
        "H4sIAMIbpGoC/52WjZKrMAiFqwkBQYjv/7T3oHZm27VrelM7dtIvh58E9PH43+EmzGw2RisJlXkulXQI18RLqbXyiIWlMAGWvsHQ"
        + "wIK+EBWJZQ0l0vsF3Rv7tk3NqY4YaNM0961Pi9dK7De0zfM0F9mWaWHw4nfOT9PUyqwixHzL+7ZOOWaEyqK3/LYuHf60tqqacbnl"
        + "+7q1udm6rr0rDfB9W1oPam3xjPcm/8n3iCNTA+nsG8bz1JXKt9sLvB84z4Xuj3ScvFFBsAMV0HsyWlNdBwvmIaX8cMYjwv9aujtz"
        + "qqsqV5Sa6EdrysBpVzf8njFgr1S5XmGCaiTNzAvRDp8LcKKueMTKmRpTOkmUJ9VcWS/qx1Ip5XHe5iQJlQ8eIVU4+r7A4X1NdyzL"
        + "PpkpPSrJoin9SnJANXlV6DJky9lcas7XX1FHHku2QBrx4/AdDSO106U84/bOZ19IHvLpPljKUYVJMC0vvOwQJYs1c+XdqZyDNLOg"
        + "sn+m1SyVjqSw7Hc+FRgT+CC21wQR7/+nAU02RTGJxgo3BbP22szxP6KDR3mIJJ0XxU3DgIvUtz1wldSl1GRxZBaweXYMyQD4NeAM"
        + "IRFFinJ/1HCazcNT2w6ttz1LTViXwy19DscI3Ph3GzDHBw7o7j321AxV03fjYVdtI1BWeWEP4Fr3+CGmHx8FAr9TPmKokBE4FqTb"
        + "Y4XfkRd79DAf5Nn3GMXGDGyy813Z/7BgcDh3pEc/+Ec0+sxHK3snxw6gQT+b9UcL3lDUuh837NT5wOgfm6lTQTXiIYArQiXOquPr"
        + "nLpA+rScJ6Af1Ops/SrUZjhPF0ohcskv4Cku33Iu/enVvGgf5gMnrPllHq7zE/t1YZiurP7xqlDtO/5L/c04vuG70Vf85l/pry7o"
        + "WYMrAmi+Pw1WWWRPQ1f1sQThJWel2BrjbSrH0Iotv0Pwx/EPdy61DQAMAAA="
    ) }

    val flWalk3 by lazy { decode(48, 64, flamingoPal,
        "H4sIAMIbpGoC/61Wi5KkMAgcDRgEIfn/r73Gmand831V56xWrXaggYbk9fqPl5mq2Uvmh/BKpVA1InuAnoVLKWMhEn2CNx55blNh"
        + "qnWKW7gLFevL4pVZw56YL7IMcw+blnYPV5DnGJZuOgzTcocPSbyNU/NpHIfRHuCFSapMw1yGYez3+IqLdBpGwOHnHq+m4vNYxqHN"
        + "y7WDqKAvHh69zePY2w3ekR+u7yg7jPcbPMRTiD+cW+89+n19Wd8Lllxwh1dObf44eKJmFvvi270iKBesyox7+OpgLPW6TC+kPOKD"
        + "QQRfB2doqQQW6ChJvFJq7tS/JIExy19SDCLpgMRO8l1XLG569y2rQUTEdsRIFLbhnZmo5gNXtZWQHISMXMNiPoeS6PUGHjXjGgep"
        + "Y4bYmcCm5N+IFVxRY0R/kCFFpJXSNAiv+ILFMJFe9niBXcY8YBDKYFfyAigf4qHE/IxH5S95BAsHuUTUd8Gu3ElyTVrMe+WeH8x9"
        + "WycVME226eKdSnWBbUbN1A+yiQWoZpKta2ZQKowSvLOd+eQPnLphjUjmVc11xeLyXXacSgKxNXhTMccPUpK0oFDrXgoYwFJVP94d"
        + "tpMdLB9Zf71QQUsK7quX1DT4uaERDrXPpK6BsACBRcsQ1L3FSWtBV/b9BKOpe8w18XbaVL8MoRnXKM3O+/B3AyE3IAI3HvFkDw15"
        + "T4QQeTBCMNU+3RFOdu8gjDS+jtjvrdOP2J2mmwXN+Nc4w4Cu14z8Lw4QyB3eWePXf8bXu2jzv0IM5PbSQdt8b359kIntaGpGVykC"
        + "/f2uGhdS4Ni7PJ//RxVtShqnW7oevaQ4y+ZRcNDdhK00pu3+ju7lI0sBRmYT6+YMl/PssEW85jB12xxCs5qKvtqdK3wdpWXavsdJ"
        + "EJPUdmdHo2hxMBUhB8bMsrltIz5JULept2nu27PgyUljWXrvD84h/3b9AecXGzAADAAA"
    ) }

    val manWalk by lazy { arrayOf(manWalk1, manWalk2, manWalk3, manWalk4) }
    val flWalk by lazy { arrayOf(flWalk1, flWalk2, flWalk3) }
}
