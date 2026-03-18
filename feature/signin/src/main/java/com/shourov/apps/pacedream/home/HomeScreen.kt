/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.graphics.SolidColor
import com.pacedream.common.composables.theme.PaceDreamColors
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shourov.apps.pacedream.feature.signin.R
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {

    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        bottomBar = {

            BottomAppBar(
                modifier = Modifier
                    .height(95.dp),
                containerColor = Color.White,

                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Companion.White,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        )
                        .padding(start = 12.dp, end = 12.dp)
                        .clip(
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_home),
                            contentDescription = "Home Icon",
                        )
                        Text(
                            text = "Home",
                            style = TextStyle(
                                fontSize = 12.sp,
                            ),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_booking),
                            contentDescription = "Home Icon",
                        )
                        Text(
                            text = "Booking",
                            style = TextStyle(
                                fontSize = 12.sp,
                            ),
                            color = Color(0xff5527D7),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_post),
                            contentDescription = "Home Icon",
                        )
                        Text(
                            text = "Post",
                            style = TextStyle(
                                fontSize = 12.sp,
                            ),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_notification),
                            contentDescription = "Home Icon",
                        )
                        Text(
                            text = "Inbox",
                            style = TextStyle(
                                fontSize = 12.sp,
                            ),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_profile),
                            contentDescription = "Home Icon",
                        )
                        Text(
                            text = "Profile",
                            style = TextStyle(
                                fontSize = 12.sp,
                            ),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

        },
        containerColor = Color.White,
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .background(color = Companion.White)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Companion.Gray,
                            shape = CircleShape,
                        )
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_fillter),
                        contentDescription = "Filter Icon",
                        modifier = Modifier.size(18.dp),
                    )

                    Text(
                        text = "Filter",
                        style = TextStyle(
                            fontSize = 14.sp,
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                BasicTextFieldDesign(
                    value = query,
                    onQueryChange = {
                        query = it
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
            TabBar()

        }

    }
}

@Composable
fun BasicTextFieldDesign(
    value: String = "",
    onQueryChange: (String) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(34.dp)
            .border(
                width = 1.dp,
                color = Companion.Gray,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(200.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "Search Icon",
                modifier = Modifier.size(18.dp),
            )

            if (value.isEmpty()) {
                Text(
                    text = "Search by booking ID",
                    style = TextStyle(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        BasicTextField(
            value = value,
            onValueChange = onQueryChange,
            modifier = Modifier.padding(start = 44.dp)
                .width(200.dp),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
fun TabBar() {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Tabs(pagerState = pagerState)
        Spacer(Modifier.height(8.dp))
        TabsContent(pagerState = pagerState)
    }
}

@Composable
fun TabsContent(pagerState: PagerState) {

    val cancelled = rememberSaveable { mutableStateOf(false) }
    val completed = rememberSaveable { mutableStateOf(false) }


    HorizontalPager(state = pagerState) { page ->
        when (page) {
            0 -> {

                dummyList.map {
                    if (it.status == "Completed") {
                        completed.value = true
                    }
                }
                if (completed.value) {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                    ) {
                        items(dummyList) {

                            if (it.status == "Completed") {

                                ShowEachCard(
                                    image = it.image,
                                    id = it.id,
                                    price = it.price,
                                    status = it.status,
                                    hotelName = it.hotelName,
                                    location = it.location,
                                    checkInDate = it.checkInDate,
                                    checkoutDate = it.checkoutDate,
                                    mainCity = it.mainCity,
                                    date = it.date,
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No Completed Booking",
                        )
                    }
                }
            }

            1 -> {

                dummyList.map {
                    if (it.status == "Cancelled") {
                        cancelled.value = true
                    }
                }
                if (cancelled.value) {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                    ) {
                        items(dummyList) {

                            if (it.status == "Cancelled") {

                                ShowEachCard(
                                    image = it.image,
                                    id = it.id,
                                    price = it.price,
                                    status = it.status,
                                    hotelName = it.hotelName,
                                    location = it.location,
                                    checkInDate = it.checkInDate,
                                    checkoutDate = it.checkoutDate,
                                    mainCity = it.mainCity,
                                    date = it.date,
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No Cancelled Booking",
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Tabs(pagerState: PagerState) {
    val tabNames = arrayOf("Completed", "Cancelled")
    val customCoroutineScope = rememberCoroutineScope()

    TabRow(
        containerColor = Color.White,
        selectedTabIndex = pagerState.currentPage,
    ) {
        tabNames.forEachIndexed { index, tabItem ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = {
                    customCoroutineScope.launch {
                        pagerState.animateScrollToPage(page = index)
                    }
                },
                text = {
                    if (pagerState.currentPage == index) {
                        Text(
                            text = tabItem,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color.Blue,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    } else {
                        Text(
                            text = tabItem,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun ShowEachCard(
    image: Int,
    id: Long,
    price: Int,
    status: String,
    hotelName: String,
    location: String,
    checkInDate: String,
    checkoutDate: String,
    mainCity: String,
    date: String,
) {

    Spacer(Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = mainCity,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = Color.Black,
        )
        Text(
            text = date,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = Companion.Gray,
        )
    }

    Spacer(Modifier.height(8.dp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Companion.Gray,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 8.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Companion.White,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ID: ${id}",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = Color.Black,
            )
            Box(
                modifier = Modifier
                    .background(
                        color = if (status == "Completed") Color(0x1A15813C) else Color(
                            0x1AFF4A4A,
                        ),
                        shape = CircleShape,
                    )
                    .padding(horizontal = 18.dp, vertical = 4.dp),
            ) {
                Text(
                    text = status,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (status == "Completed") PaceDreamColors.BookingConfirmed else PaceDreamColors.BookingCancelled,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = image),
                contentDescription = "Image",
                modifier = Modifier
                    .width(150.dp)
                    .height(130.dp)
                    .clip(
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(
                    text = hotelName,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Image(
                        painter = painterResource(id = R.drawable.ic_location),
                        contentDescription = "Location Icon",
                        modifier = Modifier.size(16.dp),
                        contentScale = ContentScale.FillBounds,
                    )
                    Text(
                        text = location,
                        style = TextStyle(
                            fontSize = 10.sp,
                        ),
                        color = Companion.Gray,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$${price}",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = "/day",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = Companion.Gray,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Check in",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = Companion.Gray,
                        )
                        Text(
                            text = checkInDate,
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Companion.Black,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Check out",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = Companion.Gray,
                        )
                        Text(
                            text = checkoutDate,
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Companion.Black,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        if (status == "Completed") {
            OutlinedButton(
                onClick = {

                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Submit your review",
                    style = TextStyle(
                        fontSize = 14.sp,
                    ),
                    color = Companion.Gray,
                )
            }
        }

        Button(
            onClick = {

            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xff5527D7),
            ),
        ) {
            Text(
                text = "Manage booking",
                style = TextStyle(
                    fontSize = 14.sp,
                ),
            )
        }
    }
}

data class dummyData(
    val image: Int,
    val price: Int,
    val id: Long,
    val status: String,
    val hotelName: String,
    val location: String,
    val checkInDate: String,
    val checkoutDate: String,
    val mainCity: String,
    val date: String,
)

// Empty list: bookings are loaded from the real API via BookingRepository
// in the main app module's BookingTabScreen. This legacy screen is no longer
// the primary booking display path.
val dummyList = emptyList<dummyData>()
